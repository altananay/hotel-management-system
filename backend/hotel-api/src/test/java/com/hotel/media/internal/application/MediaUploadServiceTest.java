package com.hotel.media.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotel.media.internal.domain.MediaErrorCodes;
import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.domain.MediaType;
import com.hotel.media.internal.persistence.MediaAssetEntity;
import com.hotel.media.internal.persistence.MediaAssetJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for presigned upload start and completion validation (AC-2, AC-3, D6). */
@ExtendWith(MockitoExtension.class)
class MediaUploadServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");
    private static final UUID MEDIA_ID = UUID.fromString("0199f2c0-0000-7000-8000-000000000401");
    private static final Duration TTL = Duration.ofMinutes(10);

    @Mock private MediaAssetJpaRepository assetRepository;
    @Mock private MediaStoragePort storage;
    @Mock private MediaImageProcessor imageProcessor;
    @Mock private MediaProcessingScheduler scheduler;

    private MediaUploadService service;

    @BeforeEach
    void setUp() {
        service =
                new MediaUploadService(
                        assetRepository,
                        storage,
                        imageProcessor,
                        scheduler,
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        TTL);
    }

    @Test
    void shouldCreatePrivateUploadAndReturnPresignedUrl() {
        when(assetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.presignPut(anyString(), eq("image/jpeg"), eq(1000L), eq(TTL)))
                .thenReturn(
                        new MediaStoragePort.PresignedUpload(
                                "http://localhost:9000/upload", NOW.plus(TTL)));

        MediaUploadService.StartResult result =
                service.start(new MediaUploadService.StartCommand("room.jpg", "IMAGE/JPEG", 1000));

        ArgumentCaptor<MediaAssetEntity> captor = ArgumentCaptor.forClass(MediaAssetEntity.class);
        verify(assetRepository).save(captor.capture());
        MediaAssetEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(result.mediaId());
        assertThat(saved.getStatus()).isEqualTo(MediaStatus.UPLOADED);
        assertThat(saved.getStorageKey()).isEqualTo("private/" + result.mediaId() + "/original");
        assertThat(saved.getRoomTypeId()).isNull();
        assertThat(result.uploadUrl()).isEqualTo("http://localhost:9000/upload");
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(TTL));
    }

    @Test
    void shouldRejectUnsupportedTypeOrSizeBeforePresigning() {
        assertThatThrownBy(
                        () ->
                                service.start(
                                        new MediaUploadService.StartCommand(
                                                "image.svg", "image/svg+xml", 1000)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.INVALID_CONTENT_TYPE));
        assertThatThrownBy(
                        () ->
                                service.start(
                                        new MediaUploadService.StartCommand(
                                                "big.jpg", "image/jpeg", 20_000_000)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.FILE_TOO_LARGE));

        verify(storage, never()).presignPut(anyString(), anyString(), anyLong(), any());
        verify(assetRepository, never()).save(any());
    }

    @Test
    void shouldValidateStoredObjectAndScheduleProcessing_whenCompleted() {
        MediaAssetEntity asset = uploadedAsset(NOW);
        byte[] bytes = new byte[1200];
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.head("private/original"))
                .thenReturn(new MediaStoragePort.StoredObject(1200, "image/jpeg"));
        when(storage.get("private/original")).thenReturn(bytes);
        when(imageProcessor.inspect(bytes))
                .thenReturn(new MediaImageProcessor.ImageInfo("image/jpeg", 1200, 800));

        MediaUploadService.CompleteResult result = service.complete(MEDIA_ID);

        assertThat(result.status()).isEqualTo(MediaStatus.PROCESSING);
        assertThat(asset.getStatus()).isEqualTo(MediaStatus.PROCESSING);
        assertThat(asset.getWidth()).isEqualTo(1200);
        assertThat(asset.getHeight()).isEqualTo(800);
        assertThat(asset.getProcessingAttempts()).isZero();
        verify(scheduler).enqueue(MEDIA_ID);
    }

    @Test
    void shouldRejectExpiredPresignedUpload() {
        MediaAssetEntity asset = uploadedAsset(NOW.minus(TTL).minusSeconds(1));
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.complete(MEDIA_ID))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.UPLOAD_EXPIRED));

        verify(storage, never()).head(anyString());
        verify(scheduler, never()).enqueue(any());
    }

    @Test
    void shouldMarkFailedAndDeleteWhenObjectIsMissing() {
        MediaAssetEntity asset = uploadedAsset(NOW);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.head("private/original")).thenReturn(null);

        assertThatThrownBy(() -> service.complete(MEDIA_ID))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.INVALID_IMAGE));

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.FAILED);
        verify(storage).delete("private/original");
        verify(scheduler, never()).enqueue(any());
    }

    @Test
    void shouldMarkFailedAndDeleteWhenMagicOrDimensionsAreInvalid() {
        MediaAssetEntity asset = uploadedAsset(NOW);
        byte[] bytes = new byte[1200];
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.head("private/original"))
                .thenReturn(new MediaStoragePort.StoredObject(1200, "image/jpeg"));
        when(storage.get("private/original")).thenReturn(bytes);
        when(imageProcessor.inspect(bytes))
                .thenThrow(
                        new DomainError(
                                MediaErrorCodes.INVALID_IMAGE,
                                "invalid",
                                com.hotel.sharedkernel.api.DomainErrorCategory.VALIDATION));

        assertThatThrownBy(() -> service.complete(MEDIA_ID))
                .isInstanceOf(DomainError.class);
        assertThat(asset.getStatus()).isEqualTo(MediaStatus.FAILED);
        assertThat(asset.getFailureCode()).isEqualTo(MediaErrorCodes.INVALID_IMAGE);
        verify(storage).delete("private/original");
    }

    @Test
    void shouldRejectCompleteForArchivedOrAlreadyProcessingMedia() {
        MediaAssetEntity archived = uploadedAsset(NOW);
        archived.archive(NOW);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> service.complete(MEDIA_ID))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.ALREADY_ARCHIVED));

        MediaAssetEntity processing = uploadedAsset(NOW);
        processing.markProcessing("image/jpeg", 1000, 1200, 800, NOW);
        when(assetRepository.findLockedById(MEDIA_ID)).thenReturn(Optional.of(processing));

        assertThatThrownBy(() -> service.complete(MEDIA_ID))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.NOT_READY));
    }

    private static MediaAssetEntity uploadedAsset(Instant createdAt) {
        return new MediaAssetEntity(
                MEDIA_ID, MediaType.PHOTO, "private/original", "image/jpeg", 1000, createdAt);
    }
}
