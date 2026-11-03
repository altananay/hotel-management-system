package com.hotel.media.internal.application;

import com.fasterxml.uuid.Generators;
import com.hotel.media.api.MediaReady;
import com.hotel.media.internal.domain.MediaErrorCodes;
import com.hotel.media.internal.domain.MediaRules;
import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.domain.MediaVariantFormat;
import com.hotel.media.internal.domain.MediaVariantType;
import com.hotel.media.internal.persistence.MediaAssetEntity;
import com.hotel.media.internal.persistence.MediaAssetJpaRepository;
import com.hotel.media.internal.persistence.MediaVariantEntity;
import com.hotel.media.internal.persistence.MediaVariantJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class MediaProcessingWorker {

    private final MediaAssetJpaRepository assetRepository;
    private final MediaVariantJpaRepository variantRepository;
    private final MediaStoragePort storage;
    private final MediaImageProcessor imageProcessor;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration staleThreshold;

    public MediaProcessingWorker(
            MediaAssetJpaRepository assetRepository,
            MediaVariantJpaRepository variantRepository,
            MediaStoragePort storage,
            MediaImageProcessor imageProcessor,
            ApplicationEventPublisher events,
            Clock clock,
            @Value("${hotel.media.processing.max-attempts:3}") int maxAttempts,
            @Value("${hotel.media.processing.stale-threshold:10m}") Duration staleThreshold) {
        this.assetRepository = assetRepository;
        this.variantRepository = variantRepository;
        this.storage = storage;
        this.imageProcessor = imageProcessor;
        this.events = events;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.staleThreshold = staleThreshold;
    }

    public void process(UUID mediaId) {
        String lastFailure = MediaErrorCodes.PROCESSING_FAILED;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Instant now = clock.instant();
            UUID leaseId = UUID.randomUUID();
            int claimed =
                    assetRepository.claimForProcessing(
                            mediaId,
                            MediaStatus.PROCESSING,
                            now,
                            now.minus(staleThreshold),
                            leaseId,
                            maxAttempts);
            if (claimed == 0) {
                markExhausted(mediaId, now, lastFailure);
                return;
            }
            try {
                if (!processOnce(mediaId, leaseId)) {
                    return;
                }
                return;
            } catch (RuntimeException ex) {
                lastFailure = failureCode(ex);
                if (assetRepository.releaseProcessingClaim(
                                mediaId, leaseId, clock.instant())
                        == 0) {
                    return;
                }
            }
        }
        markExhausted(mediaId, clock.instant(), lastFailure);
    }

    private boolean processOnce(UUID mediaId, UUID leaseId) {
        MediaAssetEntity asset =
                assetRepository
                        .findById(mediaId)
                        .orElseThrow(() -> notFound());
        MediaStoragePort.StoredObject stored = storage.head(asset.getStorageKey());
        if (stored == null) {
            throw new DomainError(
                    MediaErrorCodes.INVALID_IMAGE,
                    "The uploaded image was not found.",
                    DomainErrorCategory.VALIDATION);
        }
        if (stored.sizeBytes() > MediaRules.MAX_UPLOAD_BYTES) {
            throw new DomainError(
                    MediaErrorCodes.FILE_TOO_LARGE,
                    "The uploaded image exceeds the allowed size.",
                    DomainErrorCategory.VALIDATION);
        }
        byte[] original = storage.get(asset.getStorageKey());
        if (original.length > MediaRules.MAX_UPLOAD_BYTES) {
            throw new DomainError(
                    MediaErrorCodes.FILE_TOO_LARGE,
                    "The uploaded image exceeds the allowed size.",
                    DomainErrorCategory.VALIDATION);
        }
        MediaImageProcessor.ProcessedImage processed = imageProcessor.process(original);
        List<MediaVariantEntity> existing =
                variantRepository.findByMediaAssetId(mediaId);
        Map<String, MediaVariantEntity> byTypeAndFormat = new LinkedHashMap<>();
        for (MediaVariantEntity variant : existing) {
            byTypeAndFormat.put(
                    variant.getVariant().name() + ":" + variant.getFormat().name(), variant);
        }
        Instant now = clock.instant();
        for (MediaImageProcessor.GeneratedVariant generated : processed.variants()) {
            String extension = extension(generated.format());
            String privateKey =
                    "private/"
                            + mediaId
                            + "/"
                            + generated.variant().name().toLowerCase()
                            + "."
                            + extension;
            storage.put(privateKey, generated.bytes(), contentType(generated.format()));
            String key = generated.variant().name() + ":" + generated.format().name();
            MediaVariantEntity variant = byTypeAndFormat.remove(key);
            if (variant == null) {
                variant =
                        new MediaVariantEntity(
                                Generators.timeBasedEpochGenerator().generate(),
                                mediaId,
                                generated.variant(),
                                generated.format(),
                                privateKey,
                                generated.width(),
                                generated.height(),
                                generated.bytes().length,
                                now);
            } else {
                variant.regenerate(
                        privateKey,
                        generated.width(),
                        generated.height(),
                        generated.bytes().length);
            }
            variantRepository.save(variant);
        }
        byTypeAndFormat.values().forEach(variantRepository::delete);
        int updated = assetRepository.markReadyIfClaimed(mediaId, leaseId, now);
        if (updated == 0) {
            return false;
        }
        assetRepository
                .findById(mediaId)
                .ifPresent(
                        current ->
                                events.publishEvent(
                                        new MediaReady(
                                                current.getId(), current.getRoomTypeId())));
        return true;
    }

    private void markExhausted(UUID mediaId, Instant now, String failureCode) {
        assetRepository.markFailedIfExhausted(
                mediaId,
                failureCode,
                now,
                now.minus(staleThreshold),
                maxAttempts);
    }

    private String failureCode(RuntimeException ex) {
        if (ex instanceof DomainError domainError) {
            return domainError.code();
        }
        return MediaErrorCodes.PROCESSING_FAILED;
    }

    private String extension(MediaVariantFormat format) {
        return format == MediaVariantFormat.WEBP ? "webp" : "jpg";
    }

    private String contentType(MediaVariantFormat format) {
        return format == MediaVariantFormat.WEBP ? "image/webp" : "image/jpeg";
    }

    private DomainError notFound() {
        return new DomainError(
                MediaErrorCodes.NOT_FOUND, "Media asset was not found.", DomainErrorCategory.NOT_FOUND);
    }
}
