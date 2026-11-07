package com.hotel.media.internal.application;

import com.fasterxml.uuid.Generators;
import com.hotel.media.internal.domain.MediaErrorCodes;
import com.hotel.media.internal.domain.MediaRules;
import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.domain.MediaType;
import com.hotel.media.internal.persistence.MediaAssetEntity;
import com.hotel.media.internal.persistence.MediaAssetJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class MediaUploadService {

    private final MediaAssetJpaRepository assetRepository;
    private final MediaStoragePort storage;
    private final MediaImageProcessor imageProcessor;
    private final MediaProcessingScheduler scheduler;
    private final Clock clock;
    private final Duration presignTtl;

    public MediaUploadService(
            MediaAssetJpaRepository assetRepository,
            MediaStoragePort storage,
            MediaImageProcessor imageProcessor,
            MediaProcessingScheduler scheduler,
            Clock clock,
            @Value("${hotel.media.upload.presign-ttl:10m}") Duration presignTtl) {
        this.assetRepository = assetRepository;
        this.storage = storage;
        this.imageProcessor = imageProcessor;
        this.scheduler = scheduler;
        this.clock = clock;
        this.presignTtl = presignTtl;
    }

    @Transactional
    public StartResult start(StartCommand command) {
        String contentType = MediaRules.normalizeContentType(command.contentType());
        MediaRules.validateUploadSize(command.sizeBytes());
        Instant now = clock.instant();
        UUID id = Generators.timeBasedEpochGenerator().generate();
        String key = "private/" + id + "/original";
        MediaAssetEntity asset =
                new MediaAssetEntity(
                        id, MediaType.PHOTO, key, contentType, command.sizeBytes(), now);
        assetRepository.save(asset);
        MediaStoragePort.PresignedUpload upload =
                storage.presignPut(key, contentType, command.sizeBytes(), presignTtl);
        return new StartResult(id, upload.url(), upload.expiresAt());
    }

    @Transactional(noRollbackFor = DomainError.class)
    public CompleteResult complete(UUID mediaId) {
        MediaAssetEntity asset =
                assetRepository
                        .findLockedById(mediaId)
                        .orElseThrow(() -> notFound());
        if (asset.getArchivedAt() != null) {
            throw new DomainError(
                    MediaErrorCodes.ALREADY_ARCHIVED,
                    "Archived media cannot be completed.",
                    DomainErrorCategory.CONFLICT);
        }
        if (asset.getStatus() != MediaStatus.UPLOADED) {
            throw new DomainError(
                    MediaErrorCodes.NOT_READY,
                    "Media upload is not awaiting completion.",
                    DomainErrorCategory.CONFLICT);
        }
        Instant now = clock.instant();
        if (!now.isBefore(asset.getCreatedAt().plus(presignTtl))) {
            asset.markFailed(MediaErrorCodes.UPLOAD_EXPIRED, now);
            assetRepository.save(asset);
            safeDelete(asset.getStorageKey());
            throw new DomainError(
                    MediaErrorCodes.UPLOAD_EXPIRED,
                    "The upload URL has expired.",
                    DomainErrorCategory.VALIDATION);
        }
        try {
            MediaStoragePort.StoredObject object = storage.head(asset.getStorageKey());
            if (object == null) {
                throw new DomainError(
                        MediaErrorCodes.INVALID_IMAGE,
                        "The uploaded image was not found.",
                        DomainErrorCategory.VALIDATION);
            }
            MediaRules.validateUploadSize(object.sizeBytes());
            byte[] bytes = storage.get(asset.getStorageKey());
            MediaImageProcessor.ImageInfo info = imageProcessor.inspect(bytes);
            if (bytes.length > MediaRules.MAX_UPLOAD_BYTES) {
                throw new DomainError(
                        MediaErrorCodes.FILE_TOO_LARGE,
                        "The uploaded image exceeds the allowed size.",
                        DomainErrorCategory.VALIDATION);
            }
            asset.markProcessing(info.mimeType(), bytes.length, info.width(), info.height(), now);
            assetRepository.save(asset);
            afterCommit(() -> scheduler.enqueue(mediaId));
            return new CompleteResult(mediaId, MediaStatus.PROCESSING);
        } catch (DomainError ex) {
            asset.markFailed(ex.code(), now);
            assetRepository.save(asset);
            safeDelete(asset.getStorageKey());
            throw ex;
        }
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                });
    }

    private void safeDelete(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException ignored) {
            // The failed asset is never public; a leftover private object is operationally cleanable.
        }
    }

    private DomainError notFound() {
        return new DomainError(
                MediaErrorCodes.NOT_FOUND,
                "Media asset was not found.",
                DomainErrorCategory.NOT_FOUND);
    }

    public record StartCommand(String fileName, String contentType, long sizeBytes) {}

    public record StartResult(UUID mediaId, String uploadUrl, Instant expiresAt) {}

    public record CompleteResult(UUID mediaId, MediaStatus status) {}
}
