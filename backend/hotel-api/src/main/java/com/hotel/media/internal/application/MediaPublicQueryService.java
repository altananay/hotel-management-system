package com.hotel.media.internal.application;

import com.hotel.catalog.api.ActiveRoomType;
import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.api.RoomTypeLookup;
import com.hotel.media.internal.domain.MediaAsset;
import com.hotel.media.internal.domain.MediaErrorCodes;
import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.domain.MediaVariant;
import com.hotel.media.internal.domain.MediaVariantFormat;
import com.hotel.media.internal.domain.MediaVariantType;
import com.hotel.media.internal.persistence.MediaAssetEntity;
import com.hotel.media.internal.persistence.MediaAssetJpaRepository;
import com.hotel.media.internal.persistence.MediaVariantEntity;
import com.hotel.media.internal.persistence.MediaVariantJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MediaPublicQueryService {

    private static final Logger log = LoggerFactory.getLogger(MediaPublicQueryService.class);
    private static final int MAX_ROOM_TYPE_IDS = 50;

    private final MediaAssetJpaRepository assetRepository;
    private final MediaVariantJpaRepository variantRepository;
    private final RoomTypeLookup roomTypeLookup;
    private final MediaStoragePort storage;
    private final TransactionTemplate readOnlyTransaction;

    public MediaPublicQueryService(
            MediaAssetJpaRepository assetRepository,
            MediaVariantJpaRepository variantRepository,
            RoomTypeLookup roomTypeLookup,
            MediaStoragePort storage,
            PlatformTransactionManager transactionManager) {
        this.assetRepository = assetRepository;
        this.variantRepository = variantRepository;
        this.roomTypeLookup = roomTypeLookup;
        this.storage = storage;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
    }

    public List<PublicMediaView> findPublished(Collection<UUID> roomTypeIds) {
        try {
            // Keep the transaction boundary inside the try block: transaction start
            // (CannotCreateTransactionException) and commit (TransactionSystemException)
            // failures must map to 503 too, not only DataAccessException from the callback.
            return readOnlyTransaction.execute(status -> findPublishedInternal(roomTypeIds));
        } catch (DataAccessException | TransactionException ex) {
            // The public contract distinguishes "temporarily unavailable" (503) from an
            // unexpected internal error (500); see docs/conventions.md §2.3 and QA-07.
            log.warn("Public media query failed because a dependency is unavailable", ex);
            throw new DomainError(
                    MediaErrorCodes.SERVICE_UNAVAILABLE,
                    "Media service is temporarily unavailable.",
                    DomainErrorCategory.INTERNAL);
        }
    }

    private List<PublicMediaView> findPublishedInternal(Collection<UUID> roomTypeIds) {
        if (roomTypeIds == null || roomTypeIds.isEmpty()) {
            return List.of();
        }
        List<RoomTypeId> requested =
                roomTypeIds.stream().distinct().map(RoomTypeId::of).toList();
        if (requested.size() > MAX_ROOM_TYPE_IDS) {
            throw new DomainError(
                    "validation.invalid_request",
                    "At most 50 room type ids can be requested.",
                    DomainErrorCategory.VALIDATION);
        }
        Set<UUID> activeIds =
                roomTypeLookup.findActiveByIds(requested).stream()
                        .map(ActiveRoomType::id)
                        .map(RoomTypeId::value)
                        .collect(Collectors.toSet());
        if (activeIds.isEmpty()) {
            return List.of();
        }
        List<MediaAssetEntity> assets =
                assetRepository.findPublishedByRoomTypeIds(MediaStatus.READY, activeIds);
        if (assets.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<MediaVariant>> variantsByAsset =
                variantRepository
                        .findByMediaAssetIdIn(
                                assets.stream().map(MediaAssetEntity::getId).toList())
                        .stream()
                        .map(MediaVariantEntity::toDomain)
                        .collect(Collectors.groupingBy(MediaVariant::mediaAssetId));

        Map<UUID, List<PublicMediaView>> byRoom = new LinkedHashMap<>();
        for (MediaAssetEntity entity : assets) {
            MediaAsset asset = entity.toDomain();
            List<MediaVariant> variants = variantsByAsset.getOrDefault(asset.id(), List.of());
            Map<MediaVariantType, PublicVariant> publicVariants = selectVariants(variants);
            if (publicVariants.isEmpty()) {
                continue;
            }
            PublicMediaView view =
                    new PublicMediaView(
                            asset.id(),
                            asset.roomTypeId(),
                            asset.cover(),
                            asset.displayOrder(),
                            asset.altTextTr(),
                            asset.altTextEn(),
                            publicVariants);
            byRoom.computeIfAbsent(asset.roomTypeId(), ignored -> new ArrayList<>()).add(view);
        }
        List<PublicMediaView> result = new ArrayList<>();
        for (List<PublicMediaView> roomItems : byRoom.values()) {
            boolean hasExplicitCover =
                    roomItems.stream().anyMatch(item -> item.cover());
            for (int index = 0; index < roomItems.size(); index++) {
                PublicMediaView item = roomItems.get(index);
                boolean cover = item.cover() || (!hasExplicitCover && index == 0);
                result.add(
                        new PublicMediaView(
                                item.id(),
                                item.roomTypeId(),
                                cover,
                                item.displayOrder(),
                                item.altTextTr(),
                                item.altTextEn(),
                                item.variants()));
            }
        }
        return result;
    }

    private Map<MediaVariantType, PublicVariant> selectVariants(List<MediaVariant> variants) {
        Map<MediaVariantType, MediaVariant> selected = new EnumMap<>(MediaVariantType.class);
        for (MediaVariant variant : variants) {
            if (variant.publicStorageKey() == null) {
                continue;
            }
            MediaVariant current = selected.get(variant.variant());
            if (current == null
                    || (current.format() != MediaVariantFormat.WEBP
                            && variant.format() == MediaVariantFormat.WEBP)) {
                selected.put(variant.variant(), variant);
            }
        }
        Map<MediaVariantType, PublicVariant> result = new EnumMap<>(MediaVariantType.class);
        selected.forEach(
                (type, variant) ->
                        result.put(
                                type,
                                new PublicVariant(
                                        type,
                                        storage.publicUrl(variant.publicStorageKey()),
                                        variant.width(),
                                        variant.height(),
                                        variant.format().name().toLowerCase())));
        return result;
    }

    public record PublicMediaView(
            UUID id,
            UUID roomTypeId,
            boolean cover,
            int displayOrder,
            String altTextTr,
            String altTextEn,
            Map<MediaVariantType, PublicVariant> variants) {}

    public record PublicVariant(
            MediaVariantType variant, String url, int width, int height, String format) {}
}
