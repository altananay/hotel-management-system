package com.hotel.media.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotel.media.api.MediaReady;
import com.hotel.media.internal.domain.MediaErrorCodes;
import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.domain.MediaType;
import com.hotel.media.internal.domain.MediaVariantFormat;
import com.hotel.media.internal.domain.MediaVariantType;
import com.hotel.media.internal.persistence.MediaAssetEntity;
import com.hotel.media.internal.persistence.MediaAssetJpaRepository;
import com.hotel.media.internal.persistence.MediaVariantEntity;
import com.hotel.media.internal.persistence.MediaVariantJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** Unit tests for async processing claims, retries and variant persistence (AC-3, D3, D9). */
@ExtendWith(MockitoExtension.class)
class MediaProcessingWorkerTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");
    private static final UUID MEDIA_ID = UUID.fromString("0199f2c0-0000-7000-8000-000000000401");
    private static final Duration STALE = Duration.ofMinutes(10);

    @Mock private MediaAssetJpaRepository assetRepository;
    @Mock private MediaVariantJpaRepository variantRepository;
    @Mock private MediaStoragePort storage;
    @Mock private MediaImageProcessor imageProcessor;
    @Mock private ApplicationEventPublisher events;

    private MediaProcessingWorker worker;

    @BeforeEach
    void setUp() {
        worker =
                new MediaProcessingWorker(
                        assetRepository,
                        variantRepository,
                        storage,
                        imageProcessor,
                        events,
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        2,
                        STALE);
    }

    @Test
    void shouldMarkExhaustedWhenClaimIsLost() {
        when(assetRepository.claimForProcessing(
                        eq(MEDIA_ID),
                        eq(MediaStatus.PROCESSING),
                        eq(NOW),
                        eq(NOW.minus(STALE)),
                        any(UUID.class),
                        eq(2)))
                .thenReturn(0);

        worker.process(MEDIA_ID);

        verify(assetRepository)
                .markFailedIfExhausted(
                        eq(MEDIA_ID),
                        eq(MediaErrorCodes.PROCESSING_FAILED),
                        eq(NOW),
                        eq(NOW.minus(STALE)),
                        eq(2));
        verify(storage, never()).get(anyString());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void shouldProcessClaimedMediaAndPublishReadyEventWithFencedLease() {
        MediaAssetEntity asset = processingAsset();
        byte[] bytes = new byte[] {1, 2, 3};
        when(assetRepository.claimForProcessing(
                        eq(MEDIA_ID),
                        eq(MediaStatus.PROCESSING),
                        eq(NOW),
                        eq(NOW.minus(STALE)),
                        any(UUID.class),
                        eq(2)))
                .thenReturn(1);
        when(assetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(storage.head("private/original"))
                .thenReturn(new MediaStoragePort.StoredObject(3, "image/jpeg"));
        when(storage.get("private/original")).thenReturn(bytes);
        when(imageProcessor.process(bytes))
                .thenReturn(
                        new MediaImageProcessor.ProcessedImage(
                                1200,
                                800,
                                List.of(
                                        new MediaImageProcessor.GeneratedVariant(
                                                MediaVariantType.CARD,
                                                MediaVariantFormat.WEBP,
                                                1200,
                                                800,
                                                bytes))));
        when(assetRepository.markReadyIfClaimed(eq(MEDIA_ID), any(UUID.class), eq(NOW)))
                .thenReturn(1);

        worker.process(MEDIA_ID);

        ArgumentCaptor<MediaVariantEntity> variantCaptor =
                ArgumentCaptor.forClass(MediaVariantEntity.class);
        verify(variantRepository).save(variantCaptor.capture());
        assertThat(variantCaptor.getValue().getPrivateStorageKey())
                .isEqualTo("private/" + MEDIA_ID + "/card.webp");
        verify(storage).put("private/" + MEDIA_ID + "/card.webp", bytes, "image/webp");
        verify(variantRepository, never()).deleteByMediaAssetId(any());
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(MediaReady.class);
        verify(assetRepository, never()).releaseProcessingClaim(any(), any(), any());
    }

    @Test
    void shouldUpdateExistingVariantInPlaceAndRemoveObsoletePrivateObject() {
        MediaAssetEntity asset = processingAsset();
        byte[] bytes = new byte[] {1, 2, 3};
        UUID existingId = UUID.randomUUID();
        MediaVariantEntity existing =
                new MediaVariantEntity(
                        existingId,
                        MEDIA_ID,
                        MediaVariantType.CARD,
                        MediaVariantFormat.WEBP,
                        "private/" + MEDIA_ID + "/card.webp",
                        1200,
                        800,
                        1,
                        NOW);
        MediaVariantEntity obsolete =
                new MediaVariantEntity(
                        UUID.randomUUID(),
                        MEDIA_ID,
                        MediaVariantType.COVER,
                        MediaVariantFormat.WEBP,
                        "private/" + MEDIA_ID + "/cover.webp",
                        1920,
                        1280,
                        1,
                        NOW);
        when(assetRepository.claimForProcessing(
                        eq(MEDIA_ID),
                        eq(MediaStatus.PROCESSING),
                        eq(NOW),
                        eq(NOW.minus(STALE)),
                        any(UUID.class),
                        eq(2)))
                .thenReturn(1);
        when(assetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(storage.head("private/original"))
                .thenReturn(new MediaStoragePort.StoredObject(3, "image/jpeg"));
        when(storage.get("private/original")).thenReturn(bytes);
        when(imageProcessor.process(bytes))
                .thenReturn(
                        new MediaImageProcessor.ProcessedImage(
                                1200,
                                800,
                                List.of(
                                        new MediaImageProcessor.GeneratedVariant(
                                                MediaVariantType.CARD,
                                                MediaVariantFormat.WEBP,
                                                1200,
                                                800,
                                                bytes))));
        when(variantRepository.findByMediaAssetId(MEDIA_ID))
                .thenReturn(List.of(existing, obsolete));
        when(assetRepository.markReadyIfClaimed(eq(MEDIA_ID), any(UUID.class), eq(NOW)))
                .thenReturn(1);

        worker.process(MEDIA_ID);

        verify(variantRepository).save(existing);
        verify(variantRepository).delete(obsolete);
        verify(storage).delete("private/" + MEDIA_ID + "/cover.webp");
        assertThat(existing.getId()).isEqualTo(existingId);
    }

    @Test
    void shouldNotPublishReadyEventWhenArchiveWinsDuringProcessing() {
        MediaAssetEntity asset = processingAsset();
        byte[] bytes = new byte[] {1, 2, 3};
        when(assetRepository.claimForProcessing(
                        eq(MEDIA_ID),
                        eq(MediaStatus.PROCESSING),
                        eq(NOW),
                        eq(NOW.minus(STALE)),
                        any(UUID.class),
                        eq(2)))
                .thenReturn(1);
        when(assetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(storage.head("private/original"))
                .thenReturn(new MediaStoragePort.StoredObject(3, "image/jpeg"));
        when(storage.get("private/original")).thenReturn(bytes);
        when(imageProcessor.process(bytes))
                .thenReturn(
                        new MediaImageProcessor.ProcessedImage(
                                1200,
                                800,
                                List.of(
                                        new MediaImageProcessor.GeneratedVariant(
                                                MediaVariantType.CARD,
                                                MediaVariantFormat.WEBP,
                                                1200,
                                                800,
                                                bytes))));
        when(assetRepository.markReadyIfClaimed(eq(MEDIA_ID), any(UUID.class), eq(NOW)))
                .thenReturn(0);

        worker.process(MEDIA_ID);

        verify(events, never()).publishEvent(any());
    }

    @Test
    void shouldReleaseClaimAndRetryOnTransientFailure() {
        MediaAssetEntity asset = processingAsset();
        byte[] bytes = new byte[] {1, 2, 3};
        when(assetRepository.claimForProcessing(
                        eq(MEDIA_ID),
                        eq(MediaStatus.PROCESSING),
                        eq(NOW),
                        eq(NOW.minus(STALE)),
                        any(UUID.class),
                        eq(2)))
                .thenReturn(1, 1);
        when(assetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(storage.head("private/original"))
                .thenReturn(new MediaStoragePort.StoredObject(3, "image/jpeg"));
        when(storage.get("private/original"))
                .thenThrow(new com.hotel.media.api.MediaStorageException("down", new RuntimeException()))
                .thenReturn(bytes);
        when(imageProcessor.process(bytes))
                .thenReturn(
                        new MediaImageProcessor.ProcessedImage(
                                1200,
                                800,
                                List.of(
                                        new MediaImageProcessor.GeneratedVariant(
                                                MediaVariantType.THUMB,
                                                MediaVariantFormat.JPEG,
                                                480,
                                                320,
                                                bytes))));
        when(assetRepository.releaseProcessingClaim(eq(MEDIA_ID), any(UUID.class), eq(NOW)))
                .thenReturn(1);
        when(assetRepository.markReadyIfClaimed(eq(MEDIA_ID), any(UUID.class), eq(NOW)))
                .thenReturn(1);

        worker.process(MEDIA_ID);

        verify(assetRepository)
                .releaseProcessingClaim(eq(MEDIA_ID), any(UUID.class), eq(NOW));
        verify(assetRepository, times(2))
                .claimForProcessing(
                        eq(MEDIA_ID),
                        eq(MediaStatus.PROCESSING),
                        eq(NOW),
                        eq(NOW.minus(STALE)),
                        any(UUID.class),
                        eq(2));
        verify(assetRepository).markReadyIfClaimed(eq(MEDIA_ID), any(UUID.class), eq(NOW));
    }

    @Test
    void shouldMarkExhaustedAfterMaxAttempts() {
        MediaAssetEntity asset = processingAsset();
        when(assetRepository.claimForProcessing(
                        eq(MEDIA_ID),
                        eq(MediaStatus.PROCESSING),
                        eq(NOW),
                        eq(NOW.minus(STALE)),
                        any(UUID.class),
                        eq(2)))
                .thenReturn(1, 1);
        when(assetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(storage.head("private/original"))
                .thenReturn(new MediaStoragePort.StoredObject(3, "image/jpeg"));
        when(storage.get("private/original"))
                .thenThrow(new com.hotel.media.api.MediaStorageException("down", new RuntimeException()));
        when(assetRepository.releaseProcessingClaim(eq(MEDIA_ID), any(UUID.class), eq(NOW)))
                .thenReturn(1);

        worker.process(MEDIA_ID);

        verify(assetRepository, times(2))
                .releaseProcessingClaim(eq(MEDIA_ID), any(UUID.class), eq(NOW));
        verify(assetRepository)
                .markFailedIfExhausted(
                        eq(MEDIA_ID),
                        eq(MediaErrorCodes.PROCESSING_FAILED),
                        eq(NOW),
                        eq(NOW.minus(STALE)),
                        eq(2));
        verify(events, never()).publishEvent(any());
    }

    @Test
    void shouldRejectOversizedStoredOriginalBeforeReadingIt() {
        MediaAssetEntity asset = processingAsset();
        when(assetRepository.claimForProcessing(
                        eq(MEDIA_ID),
                        eq(MediaStatus.PROCESSING),
                        eq(NOW),
                        eq(NOW.minus(STALE)),
                        any(UUID.class),
                        eq(2)))
                .thenReturn(1, 0);
        when(assetRepository.findById(MEDIA_ID)).thenReturn(Optional.of(asset));
        when(storage.head("private/original"))
                .thenReturn(
                        new MediaStoragePort.StoredObject(
                                16L * 1024 * 1024, "image/jpeg"));
        when(assetRepository.releaseProcessingClaim(eq(MEDIA_ID), any(UUID.class), eq(NOW)))
                .thenReturn(1);

        worker.process(MEDIA_ID);

        verify(storage, never()).get(anyString());
        verify(assetRepository)
                .markFailedIfExhausted(
                        eq(MEDIA_ID),
                        eq(MediaErrorCodes.FILE_TOO_LARGE),
                        eq(NOW),
                        eq(NOW.minus(STALE)),
                        eq(2));
    }

    private static MediaAssetEntity processingAsset() {
        MediaAssetEntity asset =
                new MediaAssetEntity(
                        MEDIA_ID, MediaType.PHOTO, "private/original", "image/jpeg", 3, NOW);
        asset.markProcessing("image/jpeg", 3, 1200, 800, NOW);
        return asset;
    }
}
