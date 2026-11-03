package com.hotel.media.internal.application;

import com.fasterxml.uuid.Generators;
import com.hotel.media.api.MediaReady;
import com.hotel.media.internal.domain.MediaErrorCodes;
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
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Instant now = clock.instant();
            int claimed =
                    assetRepository.claimForProcessing(
                            mediaId, MediaStatus.PROCESSING, now, now.minus(staleThreshold));
            if (claimed == 0) {
                return;
            }
            try {
                processOnce(mediaId);
                return;
            } catch (RuntimeException ex) {
                if (attempt + 1 >= maxAttempts) {
                    markFailed(mediaId, failureCode(ex));
                    return;
                }
                assetRepository.releaseProcessingClaim(mediaId, clock.instant());
            }
        }
    }

    private void processOnce(UUID mediaId) {
        MediaAssetEntity asset =
                assetRepository
                        .findById(mediaId)
                        .orElseThrow(() -> notFound());
        byte[] original = storage.get(asset.getStorageKey());
        MediaImageProcessor.ProcessedImage processed = imageProcessor.process(original);
        variantRepository.deleteByMediaAssetId(mediaId);
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
            variantRepository.save(
                    new MediaVariantEntity(
                            Generators.timeBasedEpochGenerator().generate(),
                            mediaId,
                            generated.variant(),
                            generated.format(),
                            privateKey,
                            generated.width(),
                            generated.height(),
                            generated.bytes().length,
                            now));
        }
        asset.markReady(now);
        assetRepository.save(asset);
        events.publishEvent(new MediaReady(asset.getId(), asset.getRoomTypeId()));
    }

    private void markFailed(UUID mediaId, String failureCode) {
        assetRepository
                .findById(mediaId)
                .ifPresent(
                        asset -> {
                            asset.markFailed(failureCode, clock.instant());
                            assetRepository.save(asset);
                        });
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
