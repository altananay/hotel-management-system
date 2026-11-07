package com.hotel.media.internal.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.domain.MediaType;
import com.hotel.media.internal.persistence.MediaAssetEntity;
import com.hotel.media.internal.persistence.MediaAssetJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for restart recovery of stale PROCESSING claims (AC-3, D3). */
@ExtendWith(MockitoExtension.class)
class MediaProcessingRecoveryTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");
    private static final Duration STALE = Duration.ofMinutes(10);

    @Mock private MediaAssetJpaRepository assetRepository;
    @Mock private MediaProcessingScheduler scheduler;

    private MediaProcessingRecovery recovery;

    @BeforeEach
    void setUp() {
        recovery =
                new MediaProcessingRecovery(
                        assetRepository, scheduler, Clock.fixed(NOW, ZoneOffset.UTC), STALE);
    }

    @Test
    void shouldReenqueueOnlyStaleProcessingAssets() {
        UUID staleId = UUID.randomUUID();
        MediaAssetEntity stale =
                new MediaAssetEntity(
                        staleId, MediaType.PHOTO, "private/original", "image/jpeg", 1000, NOW);
        stale.markProcessing("image/jpeg", 1000, 1200, 800, NOW);
        when(assetRepository.findStaleProcessing(MediaStatus.PROCESSING, NOW.minus(STALE)))
                .thenReturn(List.of(stale));

        recovery.recoverStaleProcessing();

        verify(scheduler).enqueue(staleId);
    }

    @Test
    void shouldNotEnqueueWhenThereIsNoStaleAsset() {
        when(assetRepository.findStaleProcessing(MediaStatus.PROCESSING, NOW.minus(STALE)))
                .thenReturn(List.of());

        recovery.recoverStaleProcessing();

        verifyNoInteractions(scheduler);
    }
}
