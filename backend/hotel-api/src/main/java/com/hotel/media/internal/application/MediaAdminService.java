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
    private final MediaProcessingScheduler scheduler;
    private final Clock clock;

    public MediaAdminService(
            MediaAssetJpaRepository assetRepository,
            MediaVariantJpaRepository variantRepository,
            RoomTypeLookup roomTypeLookup,
            MediaStoragePort storage,
            MediaProcessingScheduler scheduler,
            Clock clock) {
        this.assetRepository = assetRepository;
        this.variantRepository = variantRepository;
        this.roomTypeLookup = roomTypeLookup;
        this.storage = storage;
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
        MediaAssetEntity asset = locked(id);
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
        assetRepository.save(asset);
        return view(asset);
    }

    @Transactional
    public void reorder(UUID roomTypeId, List<UUID> mediaIds) {
        ensureActiveRoomType(roomTypeId);
        if (mediaIds == null || mediaIds.isEmpty()) {
            throw conflict(MediaErrorCodes.ORDER_CONFLICT, "The media order list is empty.");
        }
        List<MediaAssetEntity> current =
                assetRepository.findNonArchivedByRoomTypeOrdered(roomTypeId);
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
        assetRepository.shiftDisplayOrders(roomTypeId, now);
        for (int index = 0; index < mediaIds.size(); index++) {
            assetRepository.setDisplayOrder(mediaIds.get(index), index, now);
        }
    }

    @Transactional
    public AdminMediaView cover(UUID id) {
        MediaAssetEntity asset = locked(id);
        requirePublished(asset);
        if (asset.getStatus() != MediaStatus.READY) {
            throw conflict(MediaErrorCodes.NOT_READY, "Only READY media can be a cover.");
        }
        Instant now = clock.instant();
        assetRepository.clearCoverForRoom(asset.getRoomTypeId(), now);
        asset.setCover(true, now);
        assetRepository.save(asset);
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
        try {
            for (MediaVariantEntity variant : variants) {
                String publicKey =
                        "public/"
                                + id
                                + "/"
                                + variant.getVariant().name().toLowerCase()
                                + "."
                                + (variant.getFormat().name().equals("WEBP") ? "webp" : "jpg");
                storage.copy(variant.getPrivateStorageKey(), publicKey);
                copied.add(publicKey);
                variant.publish(publicKey);
            }
            asset.publish(clock.instant());
            assetRepository.save(asset);
            variantRepository.saveAll(variants);
            return view(asset);
        } catch (RuntimeException ex) {
            copied.forEach(
                    key -> {
                        try {
                            storage.delete(key);
                        } catch (RuntimeException ignored) {
                            // Public copies are cleaned up on a best-effort basis.
                        }
                    });
            throw ex;
        }
    }

    @Transactional
    public AdminMediaView unpublish(UUID id) {
        MediaAssetEntity asset = locked(id);
        if (asset.getPublishedAt() == null) {
            return view(asset);
        }
        removePublicCopies(id);
        asset.unpublish(clock.instant());
        assetRepository.save(asset);
        return view(asset);
    }

    @Transactional
    public AdminMediaView archive(UUID id) {
        MediaAssetEntity asset = locked(id);
        if (asset.getArchivedAt() != null) {
            throw conflict(MediaErrorCodes.ALREADY_ARCHIVED, "Media is already archived.");
        }
        removePublicCopies(id);
        asset.archive(clock.instant());
        assetRepository.save(asset);
        return view(asset);
    }

    @Transactional
    public void retry(UUID id) {
        MediaAssetEntity asset = locked(id);
        if (asset.getStatus() != MediaStatus.FAILED) {
            throw conflict(
                    MediaErrorCodes.NOT_READY, "Only FAILED media can be retried.");
        }
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

    private void removePublicCopies(UUID id) {
        List<MediaVariantEntity> variants = variantRepository.findByMediaAssetId(id);
        for (MediaVariantEntity variant : variants) {
            if (variant.getPublicStorageKey() != null) {
                storage.delete(variant.getPublicStorageKey());
                variant.unpublish();
            }
        }
        variantRepository.saveAll(variants);
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
