package com.hotel.media.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotel.catalog.api.ActiveRoomType;
import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.api.RoomTypeLookup;
import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.domain.MediaType;
import com.hotel.media.internal.domain.MediaVariantFormat;
import com.hotel.media.internal.domain.MediaVariantType;
import com.hotel.media.internal.persistence.MediaAssetEntity;
import com.hotel.media.internal.persistence.MediaAssetJpaRepository;
import com.hotel.media.internal.persistence.MediaVariantEntity;
import com.hotel.media.internal.persistence.MediaVariantJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;

/** Unit tests for public media filtering, cover fallback and format preference (AC-5, Q3, Q5). */
@ExtendWith(MockitoExtension.class)
class MediaPublicQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");
    private static final UUID ROOM_TYPE_ID =
            UUID.fromString("0199f2c0-0000-7000-8000-000000000301");

    @Mock private MediaAssetJpaRepository assetRepository;
    @Mock private MediaVariantJpaRepository variantRepository;
    @Mock private RoomTypeLookup roomTypeLookup;
    @Mock private MediaStoragePort storage;
    @Mock private PlatformTransactionManager transactionManager;

    private MediaPublicQueryService service;

    @BeforeEach
    void setUp() {
        service =
                new MediaPublicQueryService(
                        assetRepository,
                        variantRepository,
                        roomTypeLookup,
                        storage,
                        transactionManager);
        lenient()
                .when(storage.publicUrl(any()))
                .thenAnswer(
                        invocation ->
                                "https://cdn.example.com/"
                                        + invocation
                                                .<String>getArgument(0)
                                                .replaceFirst("^public/", ""));
    }

    @Test
    void shouldReturnPublishedVariantsOrderedAndFillImplicitCover() {
        MediaAssetEntity first = publishedAsset(0);
        MediaAssetEntity second = publishedAsset(1);
        MediaVariantEntity firstThumbWebp =
                variant(first.getId(), MediaVariantType.THUMB, MediaVariantFormat.WEBP);
        MediaVariantEntity firstThumbJpeg =
                variant(first.getId(), MediaVariantType.THUMB, MediaVariantFormat.JPEG);
        MediaVariantEntity firstCardJpeg =
                variant(first.getId(), MediaVariantType.CARD, MediaVariantFormat.JPEG);
        MediaVariantEntity secondCoverWebp =
                variant(second.getId(), MediaVariantType.COVER, MediaVariantFormat.WEBP);
        MediaVariantEntity privateOnly =
                new MediaVariantEntity(
                        UUID.randomUUID(),
                        second.getId(),
                        MediaVariantType.CARD,
                        MediaVariantFormat.WEBP,
                        "private/" + second.getId() + "/card.webp",
                        1200,
                        800,
                        1000,
                        NOW);

        when(roomTypeLookup.findActiveByIds(anyList()))
                .thenReturn(
                        List.of(
                                new ActiveRoomType(
                                        RoomTypeId.of(ROOM_TYPE_ID), "Süit", "Suite")));
        when(assetRepository.findPublishedByRoomTypeIds(MediaStatus.READY, java.util.Set.of(ROOM_TYPE_ID)))
                .thenReturn(List.of(first, second));
        when(variantRepository.findByMediaAssetIdIn(anyList()))
                .thenReturn(
                        List.of(
                                firstCardJpeg,
                                firstThumbJpeg,
                                firstThumbWebp,
                                secondCoverWebp,
                                privateOnly));

        List<MediaPublicQueryService.PublicMediaView> result =
                service.findPublished(List.of(ROOM_TYPE_ID));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).displayOrder()).isZero();
        assertThat(result.get(0).cover()).isTrue();
        assertThat(result.get(1).cover()).isFalse();
        assertThat(result.get(0).variants())
                .containsOnlyKeys(
                        MediaVariantType.THUMB, MediaVariantType.CARD);
        assertThat(result.get(0).variants().get(MediaVariantType.THUMB).format())
                .isEqualTo("webp");
        assertThat(result.get(0).variants().get(MediaVariantType.THUMB).url())
                .isEqualTo("https://cdn.example.com/" + first.getId() + "/thumb.webp");
        assertThat(result.get(0).variants().get(MediaVariantType.CARD).format())
                .isEqualTo("jpeg");
        assertThat(result.get(1).variants()).containsOnlyKeys(MediaVariantType.COVER);
    }

    @Test
    void shouldUseExplicitCoverAndNeverReturnTwoCovers() {
        MediaAssetEntity first = publishedAsset(0);
        MediaAssetEntity second = publishedAsset(1);
        second.setCover(true, NOW);
        when(roomTypeLookup.findActiveByIds(anyList()))
                .thenReturn(
                        List.of(
                                new ActiveRoomType(
                                        RoomTypeId.of(ROOM_TYPE_ID), "Süit", "Suite")));
        when(assetRepository.findPublishedByRoomTypeIds(MediaStatus.READY, java.util.Set.of(ROOM_TYPE_ID)))
                .thenReturn(List.of(first, second));
        when(variantRepository.findByMediaAssetIdIn(anyList()))
                .thenReturn(
                        List.of(
                                variant(first.getId(), MediaVariantType.CARD, MediaVariantFormat.WEBP),
                                variant(second.getId(), MediaVariantType.CARD, MediaVariantFormat.WEBP)));

        List<MediaPublicQueryService.PublicMediaView> result =
                service.findPublished(List.of(ROOM_TYPE_ID));

        assertThat(result).filteredOn(MediaPublicQueryService.PublicMediaView::cover).hasSize(1);
        assertThat(result.get(1).cover()).isTrue();
        assertThat(result.get(0).cover()).isFalse();
    }

    @Test
    void shouldSkipAssetsWithoutPublishedVariants() {
        MediaAssetEntity asset = publishedAsset(0);
        when(roomTypeLookup.findActiveByIds(anyList()))
                .thenReturn(
                        List.of(
                                new ActiveRoomType(
                                        RoomTypeId.of(ROOM_TYPE_ID), "Süit", "Suite")));
        when(assetRepository.findPublishedByRoomTypeIds(MediaStatus.READY, java.util.Set.of(ROOM_TYPE_ID)))
                .thenReturn(List.of(asset));
        when(variantRepository.findByMediaAssetIdIn(anyList()))
                .thenReturn(
                        List.of(
                                new MediaVariantEntity(
                                        UUID.randomUUID(),
                                        asset.getId(),
                                        MediaVariantType.CARD,
                                        MediaVariantFormat.WEBP,
                                        "private/" + asset.getId() + "/card.webp",
                                        1200,
                                        800,
                                        1000,
                                        NOW)));

        assertThat(service.findPublished(List.of(ROOM_TYPE_ID))).isEmpty();
    }

    @Test
    void shouldReturnEmptyWithoutQuerying_whenNoRoomTypeIdIsProvidedOrActive() {
        assertThat(service.findPublished(List.of())).isEmpty();

        UUID unknown = UUID.fromString("0199f2c0-0000-7000-8000-000000000399");
        when(roomTypeLookup.findActiveByIds(anyList())).thenReturn(List.of());

        assertThat(service.findPublished(List.of(unknown))).isEmpty();
        verify(assetRepository, never()).findPublishedByRoomTypeIds(any(), any());
    }

    @Test
    void shouldReturnEmptyWithoutLoadingVariants_whenNoPublishedAssetExists() {
        when(roomTypeLookup.findActiveByIds(anyList()))
                .thenReturn(
                        List.of(
                                new ActiveRoomType(
                                        RoomTypeId.of(ROOM_TYPE_ID), "Süit", "Suite")));
        when(assetRepository.findPublishedByRoomTypeIds(MediaStatus.READY, java.util.Set.of(ROOM_TYPE_ID)))
                .thenReturn(List.of());

        assertThat(service.findPublished(List.of(ROOM_TYPE_ID))).isEmpty();
        verify(variantRepository, never()).findByMediaAssetIdIn(anyList());
    }

    @Test
    void shouldMapCatalogLookupFailureToServiceUnavailable() {
        when(roomTypeLookup.findActiveByIds(anyList()))
                .thenThrow(new DataAccessResourceFailureException("database down"));

        assertThatThrownBy(() -> service.findPublished(List.of(ROOM_TYPE_ID)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(
                                                com.hotel.media.internal.domain.MediaErrorCodes
                                                        .SERVICE_UNAVAILABLE));
    }

    @Test
    void shouldMapAssetQueryFailureToServiceUnavailable() {
        when(roomTypeLookup.findActiveByIds(anyList()))
                .thenReturn(
                        List.of(
                                new ActiveRoomType(
                                        RoomTypeId.of(ROOM_TYPE_ID), "Süit", "Suite")));
        when(assetRepository.findPublishedByRoomTypeIds(
                        MediaStatus.READY, java.util.Set.of(ROOM_TYPE_ID)))
                .thenThrow(new DataAccessResourceFailureException("database down"));

        assertThatThrownBy(() -> service.findPublished(List.of(ROOM_TYPE_ID)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(
                                                com.hotel.media.internal.domain.MediaErrorCodes
                                                        .SERVICE_UNAVAILABLE));
    }

    @Test
    void shouldMapTransactionStartFailureToServiceUnavailable() {
        when(transactionManager.getTransaction(any()))
                .thenThrow(new CannotCreateTransactionException("database down"));

        assertThatThrownBy(() -> service.findPublished(List.of(ROOM_TYPE_ID)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(
                                                com.hotel.media.internal.domain.MediaErrorCodes
                                                        .SERVICE_UNAVAILABLE));
    }

    @Test
    void shouldMapTransactionCommitFailureToServiceUnavailable() {
        when(roomTypeLookup.findActiveByIds(anyList())).thenReturn(List.of());
        doThrow(new TransactionSystemException("commit failed"))
                .when(transactionManager)
                .commit(any());

        assertThatThrownBy(() -> service.findPublished(List.of(ROOM_TYPE_ID)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(
                                                com.hotel.media.internal.domain.MediaErrorCodes
                                                        .SERVICE_UNAVAILABLE));
    }

    @Test
    void shouldRejectMoreThan50RoomTypeIds() {
        List<UUID> ids =
                IntStream.range(0, 51)
                        .mapToObj(index -> UUID.randomUUID())
                        .toList();

        assertThatThrownBy(() -> service.findPublished(ids))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo("validation.invalid_request"));
    }

    private static MediaAssetEntity publishedAsset(int order) {
        MediaAssetEntity asset =
                new MediaAssetEntity(
                        UUID.randomUUID(),
                        MediaType.PHOTO,
                        "private/" + UUID.randomUUID() + "/original",
                        "image/jpeg",
                        1000,
                        NOW);
        asset.markProcessing("image/jpeg", 1000, 1200, 800, NOW);
        asset.markReady(NOW);
        asset.assignRoomType(ROOM_TYPE_ID, order, NOW);
        asset.publish(NOW);
        return asset;
    }

    private static MediaVariantEntity variant(
            UUID assetId, MediaVariantType type, MediaVariantFormat format) {
        String extension = format == MediaVariantFormat.WEBP ? "webp" : "jpg";
        String name = type.name().toLowerCase();
        MediaVariantEntity variant =
                new MediaVariantEntity(
                        UUID.randomUUID(),
                        assetId,
                        type,
                        format,
                        "private/" + assetId + "/" + name + "." + extension,
                        1200,
                        800,
                        1000,
                        NOW);
        variant.publish("public/" + assetId + "/" + name + "." + extension);
        return variant;
    }
}
