package com.hotel.media.internal.application;

import com.hotel.catalog.api.ActiveRoomType;
import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.api.RoomTypeLookup;
import com.hotel.media.internal.domain.MediaAsset;
import com.hotel.media.internal.domain.MediaErrorCodes;
import com.hotel.media.internal.domain.MediaRules;
import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.domain.MediaVariant;
import com.hotel.media.internal.persistence.MediaAssetEntity;
import com.hotel.media.internal.persistence.MediaAssetJpaRepository;
import com.hotel.media.internal.persistence.MediaVariantEntity;
import com.hotel.media.internal.persistence.MediaVariantJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class MediaAdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MediaAssetJpaRepository assetRepository;
    private final MediaVariantJpaRepository variantRepository;
    private final RoomTypeLookup roomTypeLookup;
    private final MediaStoragePort storage;
    private final MediaImageProcessor imageProcessor;
    private final MediaPublicCleanupService publicCleanup;
    private final MediaProcessingScheduler scheduler;
    private final Clock clock;

    public MediaAdminService(
            MediaAssetJpaRepository assetRepository,
            MediaVariantJpaRepository variantRepository,
            RoomTypeLookup roomTypeLookup,
            MediaStoragePort storage,
            MediaImageProcessor imageProcessor,
            MediaPublicCleanupService publicCleanup,
            MediaProcessingScheduler scheduler,
            Clock clock) {
        this.assetRepository = assetRepository;
        this.variantRepository = variantRepository;
        this.roomTypeLookup = roomTypeLookup;
        this.storage = storage;
        this.imageProcessor = imageProcessor;
        this.publicCleanup = publicCleanup;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResult list(
            UUID roomTypeId,
            MediaStatus status,
            boolean includeArchived,
            int page,
            int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainError(
                    "validation.invalid_request",
                    "Page must be non-negative and size must be between 1 and 100.",
                    DomainErrorCategory.VALIDATION);
        }
        Page<MediaAssetEntity> result =
                assetRepository.findAdminPage(
                        roomTypeId, status, includeArchived, PageRequest.of(page, size));
        List<UUID> ids = result.getContent().stream().map(MediaAssetEntity::getId).toList();
        var variants =
                ids.isEmpty()
                        ? List.<MediaVariantEntity>of()
                        : variantRepository.findByMediaAssetIdIn(ids);
        var variantsByAsset =
                variants.stream()
                        .collect(
                                java.util.stream.Collectors.groupingBy(
                                        MediaVariantEntity::getMediaAssetId));
        List<AdminMediaView> items =
                result.getContent().stream()
                        .map(
                                entity ->
                                        new AdminMediaView(
                                                entity.toDomain(),
                                                variantsByAsset
                                                        .getOrDefault(entity.getId(), List.of())
                                                        .stream()
                                                        .map(MediaVariantEntity::toDomain)
                                                        .toList()))
                        .toList();
        return new PageResult(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional
    public AdminMediaView update(UUID id, UpdateCommand command) {
        if (command.roomTypeIdProvided() && command.roomTypeId() != null) {
            assetRepository.lockRoomMedia(command.roomTypeId());
        }
        MediaAssetEntity asset = locked(id);
        requireNotArchived(asset);
        String altTr =
                command.altTextTrProvided()
                        ? MediaRules.validateAltText(command.altTextTr())
                        : asset.getAltTextTr();
        String altEn =
                command.altTextEnProvided()
                        ? MediaRules.validateAltText(command.altTextEn())
                        : asset.getAltTextEn();
        Instant now = clock.instant();
        if (command.roomTypeIdProvided() && command.roomTypeId() != null) {
            ensureActiveRoomType(command.roomTypeId());
            if (asset.getPublishedAt() != null
                    && !command.roomTypeId().equals(asset.getRoomTypeId())) {
                throw conflict(
                        MediaErrorCodes.ORDER_CONFLICT,
                        "Published media cannot change its room type.");
            }
            if (asset.getRoomTypeId() == null
                    || !command.roomTypeId().equals(asset.getRoomTypeId())) {
                int order = assetRepository.maxDisplayOrder(command.roomTypeId()) + 1;
                asset.assignRoomType(command.roomTypeId(), order, now);
            }
        }
        asset.updateAltText(altTr, altEn, now);
        try {
            assetRepository.saveAndFlush(asset);
        } catch (DataIntegrityViolationException ex) {
            throw conflict(
                    MediaErrorCodes.ORDER_CONFLICT,
                    "The media order changed concurrently; refresh and retry.");
        }
        return view(asset);
    }

    @Transactional
    public void reorder(UUID roomTypeId, List<UUID> mediaIds) {
        ensureActiveRoomType(roomTypeId);
        if (mediaIds == null || mediaIds.isEmpty()) {
            throw conflict(MediaErrorCodes.ORDER_CONFLICT, "The media order list is empty.");
        }
        List<MediaAssetEntity> current = assetRepository.lockRoomMedia(roomTypeId);
        Set<UUID> currentIds =
                current.stream().map(MediaAssetEntity::getId).collect(java.util.stream.Collectors.toSet());
        if (currentIds.size() != mediaIds.size()
                || !currentIds.containsAll(mediaIds)
                || new LinkedHashSet<>(mediaIds).size() != mediaIds.size()) {
            throw conflict(
                    MediaErrorCodes.ORDER_CONFLICT,
                    "The media order must contain every non-archived media of the room type.");
        }
        Instant now = clock.instant();
        try {
            assetRepository.shiftDisplayOrders(roomTypeId, now);
            for (int index = 0; index < mediaIds.size(); index++) {
                assetRepository.setDisplayOrder(mediaIds.get(index), index, now);
            }
        } catch (DataIntegrityViolationException ex) {
            throw conflict(
                    MediaErrorCodes.ORDER_CONFLICT,
                    "The media order changed concurrently; refresh and retry.");
        }
    }

    @Transactional
    public AdminMediaView cover(UUID id) {
        UUID roomTypeId = assetRepository.findRoomTypeIdById(id).orElseThrow(this::notFound);
        if (roomTypeId != null) {
            assetRepository.lockRoomMedia(roomTypeId);
        }
        MediaAssetEntity asset = locked(id);
        requirePublished(asset);
        if (asset.getStatus() != MediaStatus.READY) {
            throw conflict(MediaErrorCodes.NOT_READY, "Only READY media can be a cover.");
        }
        Instant now = clock.instant();
        assetRepository.clearCoverForRoom(asset.getRoomTypeId(), now);
        asset.setCover(true, now);
        try {
            assetRepository.saveAndFlush(asset);
        } catch (DataIntegrityViolationException ex) {
            throw conflict(
                    MediaErrorCodes.COVER_CONFLICT,
                    "The cover changed concurrently; refresh and retry.");
        }
        return view(asset);
    }

    @Transactional
    public AdminMediaView publish(UUID id) {
        MediaAssetEntity asset = locked(id);
        if (asset.getStatus() != MediaStatus.READY
                || asset.getRoomTypeId() == null
                || asset.getArchivedAt() != null) {
            throw conflict(
                    MediaErrorCodes.NOT_READY,
                    "Only READY, assigned, non-archived media can be published.");
        }
        ensureActiveRoomType(asset.getRoomTypeId());
        if (asset.getPublishedAt() != null) {
            return view(asset);
        }
        List<MediaVariantEntity> variants = variantRepository.findByMediaAssetId(id);
        if (variants.isEmpty()) {
            throw conflict(MediaErrorCodes.NOT_READY, "Media has no generated variants.");
        }
        List<String> copied = new ArrayList<>();
        String publicationToken = UUID.randomUUID().toString();
        List<String> plannedKeys =
                variants.stream()
                        .map(variant -> publicKey(id, publicationToken, variant))
                        .toList();
        // Commit cleanup intents before touching S3. They are disarmed in the publish
        // transaction only if every copy and the DB commit succeed.
        publicCleanup.enqueueAllInNewTransaction(plannedKeys);
        afterRollback(
                () -> {
                    publicCleanup.enqueueAllInNewTransaction(copied);
                    publicCleanup.processNow(copied);
                });
        try {
            for (MediaVariantEntity variant : variants) {
                String publicKey = publicKey(id, publicationToken, variant);
                storage.copy(variant.getPrivateStorageKey(), publicKey);
                copied.add(publicKey);
                variant.publish(publicKey);
            }
            asset.publish(clock.instant());
            assetRepository.save(asset);
            variantRepository.saveAll(variants);
            publicCleanup.removeAll(plannedKeys);
            return view(asset);
        } catch (RuntimeException ex) {
            publicCleanup.enqueueAllInNewTransaction(copied);
            publicCleanup.processNow(copied);
            throw ex;
        }
    }

    private static String publicKey(
            UUID id, String publicationToken, MediaVariantEntity variant) {
        return "public/"
                + id
                + "/"
                + publicationToken
                + "/"
                + variant.getVariant().name().toLowerCase()
                + "."
                + (variant.getFormat().name().equals("WEBP") ? "webp" : "jpg");
    }

    @Transactional
    public AdminMediaView unpublish(UUID id) {
        MediaAssetEntity asset = locked(id);
        if (asset.getPublishedAt() == null) {
            return view(asset);
        }
        List<String> publicKeys = detachPublicCopies(id);
        asset.unpublish(clock.instant());
        assetRepository.save(asset);
        afterCommit(() -> publicCleanup.processNow(publicKeys));
        return view(asset);
    }

    @Transactional
    public AdminMediaView archive(UUID id) {
        MediaAssetEntity asset = locked(id);
        if (asset.getArchivedAt() != null) {
            throw conflict(MediaErrorCodes.ALREADY_ARCHIVED, "Media is already archived.");
        }
        List<String> publicKeys = detachPublicCopies(id);
        asset.archive(clock.instant());
        assetRepository.save(asset);
        afterCommit(() -> publicCleanup.processNow(publicKeys));
        return view(asset);
    }

    @Transactional
    public void retry(UUID id) {
        MediaAssetEntity asset = locked(id);
        requireNotArchived(asset);
        if (asset.getStatus() != MediaStatus.FAILED) {
            throw conflict(
                    MediaErrorCodes.NOT_READY, "Only FAILED media can be retried.");
        }
        validateOriginal(asset);
        asset.prepareRetry(clock.instant());
        assetRepository.save(asset);
        afterCommit(() -> scheduler.enqueue(id));
    }

    @Transactional(readOnly = true)
    public List<ActiveRoomType> activeRoomTypes() {
        return roomTypeLookup.findAllActive();
    }

    public AdminMediaView view(MediaAssetEntity asset) {
        return new AdminMediaView(
                asset.toDomain(),
                variantRepository.findByMediaAssetId(asset.getId()).stream()
                        .map(MediaVariantEntity::toDomain)
                        .toList());
    }

    private void validateOriginal(MediaAssetEntity asset) {
        MediaStoragePort.StoredObject stored = storage.head(asset.getStorageKey());
        if (stored == null || stored.sizeBytes() <= 0) {
            throw new DomainError(
                    MediaErrorCodes.INVALID_IMAGE,
                    "The original image was not found.",
                    DomainErrorCategory.VALIDATION);
        }
        if (stored.sizeBytes() > MediaRules.MAX_UPLOAD_BYTES) {
            throw new DomainError(
                    MediaErrorCodes.INVALID_IMAGE,
                    "The original image exceeds the allowed size.",
                    DomainErrorCategory.VALIDATION);
        }
        byte[] bytes = storage.get(asset.getStorageKey());
        try {
            imageProcessor.inspect(bytes);
        } catch (DomainError ex) {
            throw new DomainError(
                    MediaErrorCodes.INVALID_IMAGE,
                    "The original image is invalid.",
                    DomainErrorCategory.VALIDATION);
        }
    }

    private void requireNotArchived(MediaAssetEntity asset) {
        if (asset.getArchivedAt() != null) {
            throw conflict(MediaErrorCodes.ALREADY_ARCHIVED, "Archived media is immutable.");
        }
    }

    private List<String> detachPublicCopies(UUID id) {
        List<MediaVariantEntity> variants = variantRepository.findByMediaAssetId(id);
        List<String> keys = new ArrayList<>();
        for (MediaVariantEntity variant : variants) {
            if (variant.getPublicStorageKey() != null) {
                keys.add(variant.getPublicStorageKey());
                variant.unpublish();
            }
        }
        variantRepository.saveAll(variants);
        publicCleanup.enqueueAll(keys);
        return keys;
    }

    private MediaAssetEntity locked(UUID id) {
        return assetRepository.findLockedById(id).orElseThrow(this::notFound);
    }

    private void requirePublished(MediaAssetEntity asset) {
        if (asset.getPublishedAt() == null || asset.getArchivedAt() != null) {
            throw conflict(
                    MediaErrorCodes.NOT_PUBLISHED,
                    "Only published media can be selected as cover.");
        }
    }

    private void ensureActiveRoomType(UUID id) {
        if (roomTypeLookup.findActive(RoomTypeId.of(id)).isEmpty()) {
            throw new DomainError(
                    MediaErrorCodes.ROOM_TYPE_NOT_FOUND,
                    "The room type is not active.",
                    DomainErrorCategory.NOT_FOUND);
        }
    }

    private DomainError notFound() {
        return new DomainError(
                MediaErrorCodes.NOT_FOUND,
                "Media asset was not found.",
                DomainErrorCategory.NOT_FOUND);
    }

    private DomainError conflict(String code, String message) {
        return new DomainError(code, message, DomainErrorCategory.CONFLICT);
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

    private void afterRollback(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            action.run();
                        }
                    }
                });
    }

    public record PageResult(
            List<AdminMediaView> items, int page, int size, long total) {}

    public record AdminMediaView(MediaAsset asset, List<MediaVariant> variants) {}

    public record UpdateCommand(
            boolean roomTypeIdProvided,
            UUID roomTypeId,
            boolean altTextTrProvided,
            String altTextTr,
            boolean altTextEnProvided,
            String altTextEn) {}
}
