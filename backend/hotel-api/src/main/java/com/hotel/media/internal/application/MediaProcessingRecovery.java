package com.hotel.media.internal.application;

import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.persistence.MediaAssetJpaRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MediaProcessingRecovery {

    private final MediaAssetJpaRepository assetRepository;
    private final MediaProcessingScheduler scheduler;
    private final Clock clock;
    private final Duration staleThreshold;

    public MediaProcessingRecovery(
            MediaAssetJpaRepository assetRepository,
            MediaProcessingScheduler scheduler,
            Clock clock,
            @Value("${hotel.media.processing.stale-threshold:10m}") Duration staleThreshold) {
        this.assetRepository = assetRepository;
        this.scheduler = scheduler;
        this.clock = clock;
        this.staleThreshold = staleThreshold;
    }

    @Scheduled(
            initialDelayString = "${hotel.media.processing.recovery-initial-delay:30s}",
            fixedDelayString = "${hotel.media.processing.recovery-interval:60s}")
    public void recoverStaleProcessing() {
        assetRepository
                .findStaleProcessing(
                        MediaStatus.PROCESSING, clock.instant().minus(staleThreshold))
                .forEach(asset -> scheduler.enqueue(asset.getId()));
    }
}
