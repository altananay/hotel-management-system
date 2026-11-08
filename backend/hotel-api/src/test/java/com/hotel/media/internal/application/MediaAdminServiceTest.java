package com.hotel.media.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotel.catalog.api.ActiveRoomType;
import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.api.RoomTypeLookup;
import com.hotel.media.internal.domain.MediaErrorCodes;
import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.domain.MediaType;
import com.hotel.media.internal.domain.MediaVariantFormat;
import com.hotel.media.internal.domain.MediaVariantType;
import com.hotel.media.internal.persistence.MediaAssetEntity;
import com.hotel.media.internal.persistence.MediaAssetJpaRepository;
import com.hotel.media.internal.persistence.MediaVariantEntity;
import com.hotel.media.internal.persistence.MediaVariantJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for admin media lifecycle rules (AC-4, Q7, Q8). */
@ExtendWith(MockitoExtension.class)
class MediaAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");
    private static final UUID ROOM_TYPE_ID =
            UUID.fromString("0199f2c0-0000-7000-8000-000000000301");
    private static final UUID MEDIA_ID = UUID.fromString("0199f2c0-0000-7000-8000-000000000401");

    @Mock private MediaAssetJpaRepository assetRepository;
    @Mock private MediaVariantJpaRepository variantRepository;
    @Mock private RoomTypeLookup roomTypeLookup;
    @Mock private MediaStoragePort storage;
    @Mock private MediaImageProcessor imageProcessor;
    @Mock private MediaPublicCleanupService publicCleanup;
    @Mock private MediaProcessingScheduler scheduler;

    private MediaAdminService service;

    @BeforeEach
    void setUp() {
        service =
                new MediaAdminService(
                        assetRepository,
                        variantRepository,
                        roomTypeLookup,
                        storage,
                        imageProcessor,
                        publicCleanup,
                        scheduler,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldRejectPaginationOutsideAllowedBounds() {
        assertThatThrownBy(() -> service.list(null, null, false, -1, 20))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo("validation.invalid_request"));
        assertThatThrownBy(() -> service.list(null, null, false, 0, 101))
                .isInstanceOf(DomainError.class);

        verify(assetRepository, never()).findAdminPage(any(), any(), anyBoolean(), any());
    }

    @Test
    void shouldAppendRoomAssignmentAtMaxOrderAndUpdateAltText() {
        MediaAssetEntity asset = readyAsset(null, 0);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(roomTypeLookup.findActive(RoomTypeId.of(ROOM_TYPE_ID)))
                .thenReturn(Optional.of(activeRoom()));
        when(assetRepository.maxDisplayOrder(ROOM_TYPE_ID)).thenReturn(4);
        when(variantRepository.findByMediaAssetId(MEDIA_ID)).thenReturn(List.of());

        service.update(
                MEDIA_ID,
                new MediaAdminService.UpdateCommand(
                        true, ROOM_TYPE_ID, true, " Taş süit ", true, "Stone suite"));

        assertThat(asset.getRoomTypeId()).isEqualTo(ROOM_TYPE_ID);
        assertThat(asset.getDisplayOrder()).isEqualTo(5);
        assertThat(asset.getAltTextTr()).isEqualTo("Taş süit");
        assertThat(asset.getAltTextEn()).isEqualTo("Stone suite");
        verify(assetRepository).saveAndFlush(asset);
    }

    @Test
    void shouldKeepExistingAltText_whenOnlyRoomTypeIsProvided() {
        MediaAssetEntity asset = readyAsset(ROOM_TYPE_ID, 1);
        asset.updateAltText("Mevcut TR", "Existing EN", NOW);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(variantRepository.findByMediaAssetId(MEDIA_ID)).thenReturn(List.of());

        service.update(
                MEDIA_ID,
                new MediaAdminService.UpdateCommand(true, ROOM_TYPE_ID, false, null, false, null));

        assertThat(asset.getAltTextTr()).isEqualTo("Mevcut TR");
        assertThat(asset.getAltTextEn()).isEqualTo("Existing EN");
    }

    @Test
    void shouldAllowAltTextUpdateWhenAssignedRoomTypeIsNoLongerActive() {
        MediaAssetEntity asset = readyAsset(ROOM_TYPE_ID, 1);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(variantRepository.findByMediaAssetId(MEDIA_ID)).thenReturn(List.of());

        service.update(
                MEDIA_ID,
                new MediaAdminService.UpdateCommand(
                        true, ROOM_TYPE_ID, true, "Yeni TR", true, "New EN"));

        assertThat(asset.getAltTextTr()).isEqualTo("Yeni TR");
        assertThat(asset.getAltTextEn()).isEqualTo("New EN");
        verify(roomTypeLookup, never()).findActive(any());
    }

    @Test
    void shouldRejectAssignmentToDifferentInactiveRoomType() {
        MediaAssetEntity asset = readyAsset(ROOM_TYPE_ID, 1);
        UUID inactiveRoom =
                UUID.fromString("0199f2c0-0000-7000-8000-000000000304");
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(roomTypeLookup.findActive(RoomTypeId.of(inactiveRoom)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.update(
                                        MEDIA_ID,
                                        new MediaAdminService.UpdateCommand(
                                                true, inactiveRoom, false, null, false, null)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.ROOM_TYPE_NOT_FOUND));
        assertThat(asset.getRoomTypeId()).isEqualTo(ROOM_TYPE_ID);
    }

    @Test
    void shouldTranslateUniqueConstraintToOrderConflictOnAssignment() {
        MediaAssetEntity asset = readyAsset(null, 0);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(roomTypeLookup.findActive(RoomTypeId.of(ROOM_TYPE_ID)))
                .thenReturn(Optional.of(activeRoom()));
        when(assetRepository.maxDisplayOrder(ROOM_TYPE_ID)).thenReturn(0);
        when(assetRepository.saveAndFlush(asset))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(
                        () ->
                                service.update(
                                        MEDIA_ID,
                                        new MediaAdminService.UpdateCommand(
                                                true, ROOM_TYPE_ID, false, null, false, null)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.ORDER_CONFLICT));
    }

    @Test
    void shouldRejectRoomChangeForPublishedMedia() {
        MediaAssetEntity asset = readyAsset(ROOM_TYPE_ID, 0);
        asset.publish(NOW);
        UUID otherRoom = UUID.fromString("0199f2c0-0000-7000-8000-000000000302");
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(roomTypeLookup.findActive(RoomTypeId.of(otherRoom)))
                .thenReturn(Optional.of(activeRoom()));

        assertThatThrownBy(
                        () ->
                                service.update(
                                        MEDIA_ID,
                                        new MediaAdminService.UpdateCommand(
                                                true, otherRoom, false, null, false, null)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.ORDER_CONFLICT));
    }

    @Test
    void shouldRejectReorderUnlessCompleteListOfNonArchivedMedia() {
        MediaAssetEntity first = readyAsset(ROOM_TYPE_ID, 0);
        MediaAssetEntity second = readyAsset(ROOM_TYPE_ID, 1);
        ReflectionId.set(first, MEDIA_ID);
        ReflectionId.set(
                second, UUID.fromString("0199f2c0-0000-7000-8000-000000000402"));
        when(roomTypeLookup.findActive(RoomTypeId.of(ROOM_TYPE_ID)))
                .thenReturn(Optional.of(activeRoom()));
        when(assetRepository.lockRoomMedia(ROOM_TYPE_ID))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.reorder(ROOM_TYPE_ID, List.of(MEDIA_ID)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.ORDER_CONFLICT));
        verify(assetRepository, never()).shiftDisplayOrders(any(), any());
    }

    @Test
    void shouldShiftAndApplyCompleteReorderAtomically() {
        MediaAssetEntity first = readyAsset(ROOM_TYPE_ID, 0);
        MediaAssetEntity second = readyAsset(ROOM_TYPE_ID, 1);
        UUID secondId = UUID.fromString("0199f2c0-0000-7000-8000-000000000402");
        ReflectionId.set(first, MEDIA_ID);
        ReflectionId.set(second, secondId);
        when(roomTypeLookup.findActive(RoomTypeId.of(ROOM_TYPE_ID)))
                .thenReturn(Optional.of(activeRoom()));
        when(assetRepository.lockRoomMedia(ROOM_TYPE_ID))
                .thenReturn(List.of(first, second));

        service.reorder(ROOM_TYPE_ID, List.of(secondId, MEDIA_ID));

        verify(assetRepository).shiftDisplayOrders(ROOM_TYPE_ID, NOW);
        verify(assetRepository).setDisplayOrder(secondId, 0, NOW);
        verify(assetRepository).setDisplayOrder(MEDIA_ID, 1, NOW);
    }

    @Test
    void shouldPublishReadyAssignedMediaAndCopyEveryVariantWithPublicKeys() {
        MediaAssetEntity asset = readyAsset(ROOM_TYPE_ID, 0);
        MediaVariantEntity webp = variant(MediaVariantType.CARD, MediaVariantFormat.WEBP);
        MediaVariantEntity jpeg = variant(MediaVariantType.CARD, MediaVariantFormat.JPEG);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(roomTypeLookup.findActive(RoomTypeId.of(ROOM_TYPE_ID)))
                .thenReturn(Optional.of(activeRoom()));
        when(variantRepository.findByMediaAssetId(MEDIA_ID)).thenReturn(List.of(webp, jpeg));
        when(variantRepository.saveAll(any())).thenReturn(List.of(webp, jpeg));

        service.publish(MEDIA_ID);

        assertThat(asset.getPublishedAt()).isEqualTo(NOW);
        verify(storage)
                .copy(
                        eq("private/" + MEDIA_ID + "/card.webp"),
                        argThat(key -> key.startsWith("public/" + MEDIA_ID + "/") && key.endsWith("/card.webp")));
        verify(storage)
                .copy(
                        eq("private/" + MEDIA_ID + "/card.jpg"),
                        argThat(key -> key.startsWith("public/" + MEDIA_ID + "/") && key.endsWith("/card.jpg")));
        assertThat(webp.getPublicStorageKey())
                .startsWith("public/" + MEDIA_ID + "/")
                .endsWith("/card.webp");
        assertThat(jpeg.getPublicStorageKey())
                .startsWith("public/" + MEDIA_ID + "/")
                .endsWith("/card.jpg");
    }

    @Test
    void shouldRejectPublishWhenMediaIsNotReadyOrUnassigned() {
        MediaAssetEntity failed =
                new MediaAssetEntity(MEDIA_ID, MediaType.PHOTO, "private/original", "image/jpeg", 10, NOW);
        failed.markFailed(MediaErrorCodes.PROCESSING_FAILED, NOW);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(failed));

        assertThatThrownBy(() -> service.publish(MEDIA_ID))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.NOT_READY));
    }

    @Test
    void shouldRejectPublishWhenAssignedRoomTypeIsNoLongerActive() {
        MediaAssetEntity asset = readyAsset(ROOM_TYPE_ID, 0);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(roomTypeLookup.findActive(RoomTypeId.of(ROOM_TYPE_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(MEDIA_ID))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.ROOM_TYPE_NOT_FOUND));
        assertThat(asset.getPublishedAt()).isNull();
        verify(storage, never()).copy(any(), any());
    }

    @Test
    void shouldOnlyAllowPublishedReadyNonArchivedMediaAsCover() {
        MediaAssetEntity available = readyAsset(ROOM_TYPE_ID, 0);
        available.publish(NOW);
        when(assetRepository.findRoomTypeIdsById(MEDIA_ID))
                .thenReturn(List.of(ROOM_TYPE_ID));
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(available));
        when(assetRepository.lockRoomMedia(ROOM_TYPE_ID)).thenReturn(List.of(available));
        when(variantRepository.findByMediaAssetId(MEDIA_ID)).thenReturn(List.of());

        service.cover(MEDIA_ID);

        verify(assetRepository).clearCoverForRoom(ROOM_TYPE_ID, NOW);
        assertThat(available.isCover()).isTrue();

        MediaAssetEntity draft = readyAsset(ROOM_TYPE_ID, 0);
        when(assetRepository.findRoomTypeIdsById(MEDIA_ID))
                .thenReturn(List.of(ROOM_TYPE_ID));
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.cover(MEDIA_ID))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.NOT_PUBLISHED));
    }

    @Test
    void shouldRejectCoverAsNotPublishedWhenExistingMediaHasNoRoomType() {
        MediaAssetEntity draft = readyAsset(null, 0);
        when(assetRepository.findRoomTypeIdsById(MEDIA_ID))
                .thenReturn(Collections.singletonList(null));
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.cover(MEDIA_ID))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.NOT_PUBLISHED));
        verify(assetRepository, never()).lockRoomMedia(any());
        verify(assetRepository, never()).clearCoverForRoom(any(), any());
    }

    @Test
    void shouldReportNotFoundOnlyWhenCoverAssetDoesNotExist() {
        when(assetRepository.findRoomTypeIdsById(MEDIA_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.cover(MEDIA_ID))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.NOT_FOUND));
        verify(assetRepository, never()).findLockedById(any());
    }

    @Test
    void shouldNotResurrectArchivedMediaWhenCoverLosesTheRace() {
        MediaAssetEntity archived = readyAsset(ROOM_TYPE_ID, 0);
        archived.publish(NOW);
        archived.archive(NOW);
        when(assetRepository.findRoomTypeIdsById(MEDIA_ID))
                .thenReturn(List.of(ROOM_TYPE_ID));
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> service.cover(MEDIA_ID))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.NOT_PUBLISHED));
        verify(assetRepository, never()).clearCoverForRoom(any(), any());
        verify(assetRepository, never()).saveAndFlush(any());
        assertThat(archived.getArchivedAt()).isNotNull();
    }

    @Test
    void shouldRemovePublicCopiesWhenUnpublishing() {
        MediaAssetEntity asset = readyAsset(ROOM_TYPE_ID, 0);
        asset.publish(NOW);
        asset.setCover(true, NOW);
        MediaVariantEntity variant = variant(MediaVariantType.CARD, MediaVariantFormat.WEBP);
        variant.publish("public/" + MEDIA_ID + "/card.webp");
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(variantRepository.findByMediaAssetId(MEDIA_ID)).thenReturn(List.of(variant));

        service.unpublish(MEDIA_ID);

        verify(publicCleanup).enqueueAll(List.of("public/" + MEDIA_ID + "/card.webp"));
        verify(publicCleanup).processNow(List.of("public/" + MEDIA_ID + "/card.webp"));
        assertThat(asset.getPublishedAt()).isNull();
        assertThat(asset.isCover()).isFalse();
        assertThat(variant.getPublicStorageKey()).isNull();
    }

    @Test
    void shouldArchiveWithoutPermanentDeletionAndRejectSecondArchive() {
        MediaAssetEntity asset = readyAsset(ROOM_TYPE_ID, 0);
        asset.publish(NOW);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(variantRepository.findByMediaAssetId(MEDIA_ID)).thenReturn(List.of());

        service.archive(MEDIA_ID);

        assertThat(asset.getArchivedAt()).isEqualTo(NOW);
        assertThat(asset.getPublishedAt()).isNull();
        verify(assetRepository, never()).deleteById(any());

        assertThatThrownBy(() -> service.archive(MEDIA_ID))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.ALREADY_ARCHIVED));
    }

    @Test
    void shouldRejectRetryWhenOriginalIsInvalidAndNotEnqueue() {
        MediaAssetEntity asset = readyAsset(ROOM_TYPE_ID, 0);
        asset.markFailed(MediaErrorCodes.INVALID_IMAGE, NOW);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(storage.head("private/original"))
                .thenReturn(new MediaStoragePort.StoredObject(1000, "image/jpeg"));
        when(storage.get("private/original")).thenReturn(new byte[] {1, 2, 3});
        when(imageProcessor.inspect(any()))
                .thenThrow(
                        new DomainError(
                                MediaErrorCodes.INVALID_IMAGE,
                                "invalid",
                                com.hotel.sharedkernel.api.DomainErrorCategory.VALIDATION));

        assertThatThrownBy(() -> service.retry(MEDIA_ID))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.INVALID_IMAGE));
        assertThat(asset.getStatus()).isEqualTo(MediaStatus.FAILED);
        verify(scheduler, never()).enqueue(any());
    }

    @Test
    void shouldResetFailureAndRenqueueOnlyFailedMedia() {
        MediaAssetEntity asset = readyAsset(ROOM_TYPE_ID, 0);
        asset.markFailed(MediaErrorCodes.INVALID_IMAGE, NOW);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(storage.head("private/original"))
                .thenReturn(new MediaStoragePort.StoredObject(1000, "image/jpeg"));
        when(storage.get("private/original")).thenReturn(new byte[] {1, 2, 3});
        when(imageProcessor.inspect(any()))
                .thenReturn(new MediaImageProcessor.ImageInfo("image/jpeg", 1200, 800));

        service.retry(MEDIA_ID);

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.PROCESSING);
        assertThat(asset.getFailureCode()).isNull();
        verify(scheduler).enqueue(MEDIA_ID);

        MediaAssetEntity ready = readyAsset(ROOM_TYPE_ID, 0);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(ready));

        assertThatThrownBy(() -> service.retry(MEDIA_ID))
                .isInstanceOf(DomainError.class);
    }

    private static MediaAssetEntity readyAsset(UUID roomTypeId, int order) {
        MediaAssetEntity asset =
                new MediaAssetEntity(
                        MEDIA_ID, MediaType.PHOTO, "private/original", "image/jpeg", 1000, NOW);
        asset.markProcessing("image/jpeg", 1000, 1200, 800, NOW);
        asset.markReady(NOW);
        if (roomTypeId != null) {
            asset.assignRoomType(roomTypeId, order, NOW);
        }
        return asset;
    }

    private static ActiveRoomType activeRoom() {
        return new ActiveRoomType(
                RoomTypeId.of(ROOM_TYPE_ID), "Kemerli Süit", "Arched Suite", 2);
    }

    private static MediaVariantEntity variant(
            MediaVariantType type, MediaVariantFormat format) {
        String extension = format == MediaVariantFormat.WEBP ? "webp" : "jpg";
        return new MediaVariantEntity(
                UUID.randomUUID(),
                MEDIA_ID,
                type,
                format,
                "private/" + MEDIA_ID + "/card." + extension,
                1200,
                800,
                1000,
                NOW);
    }

    /** Sets the final id when a helper needs a distinct second asset. */
    private static final class ReflectionId {
        private static void set(MediaAssetEntity asset, UUID id) {
            org.springframework.test.util.ReflectionTestUtils.setField(asset, "id", id);
        }
    }
}
