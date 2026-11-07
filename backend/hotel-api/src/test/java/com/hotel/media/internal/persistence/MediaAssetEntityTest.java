package com.hotel.media.internal.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.domain.MediaType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for the media asset lifecycle (AC-1, AC-3, AC-4). */
class MediaAssetEntityTest {

    private static final UUID ID = UUID.fromString("0199f2c0-0000-7000-8000-000000000401");
    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");

    private MediaAssetEntity newAsset() {
        return new MediaAssetEntity(ID, MediaType.PHOTO, "private/original", "image/jpeg", 1000, NOW);
    }

    @Test
    void shouldStartAsUploadedWithZeroAttemptsAndOrder() {
        MediaAssetEntity asset = newAsset();

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.UPLOADED);
        assertThat(asset.getProcessingAttempts()).isZero();
        assertThat(asset.getDisplayOrder()).isZero();
        assertThat(asset.isCover()).isFalse();
        assertThat(asset.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldResetAttemptsWhenProcessingStartsAgain() {
        MediaAssetEntity asset = newAsset();
        ReflectionTestUtils.setField(asset, "processingAttempts", (short) 3);

        asset.markProcessing("image/jpeg", 1200, 1200, 800, NOW);

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.PROCESSING);
        assertThat(asset.getProcessingAttempts()).isZero();
        assertThat(asset.getWidth()).isEqualTo(1200);
        assertThat(asset.getHeight()).isEqualTo(800);
        assertThat(asset.getProcessingStartedAt()).isNull();
    }

    @Test
    void shouldClearFailureAndLeaseWhenReady() {
        MediaAssetEntity asset = newAsset();
        ReflectionTestUtils.setField(asset, "processingStartedAt", NOW);
        asset.markFailed("media.processing_failed", NOW);
        ReflectionTestUtils.setField(asset, "processingStartedAt", NOW);

        asset.markReady(NOW);

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.READY);
        assertThat(asset.getFailureCode()).isNull();
        assertThat(asset.getProcessingStartedAt()).isNull();
    }

    @Test
    void shouldReturnToFailedWithCode() {
        MediaAssetEntity asset = newAsset();

        asset.markFailed("media.invalid_image", NOW);

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.FAILED);
        assertThat(asset.getFailureCode()).isEqualTo("media.invalid_image");
    }

    @Test
    void shouldKeepFailureCodeUntilRetryStarts() {
        MediaAssetEntity asset = newAsset();
        asset.markFailed("media.processing_failed", NOW);
        ReflectionTestUtils.setField(asset, "processingAttempts", (short) 3);

        asset.prepareRetry(NOW);

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.PROCESSING);
        assertThat(asset.getFailureCode()).isNull();
        assertThat(asset.getProcessingAttempts()).isZero();
        assertThat(asset.getProcessingStartedAt()).isNull();
    }

    @Test
    void shouldAppendRoomAssignmentOrderAndUpdateAltText() {
        MediaAssetEntity asset = newAsset();

        asset.assignRoomType(UUID.randomUUID(), 4, NOW);
        asset.updateAltText("Taş oda", "Stone room", NOW);

        assertThat(asset.getDisplayOrder()).isEqualTo(4);
        assertThat(asset.getAltTextTr()).isEqualTo("Taş oda");
        assertThat(asset.getAltTextEn()).isEqualTo("Stone room");
    }

    @Test
    void shouldClearCoverAndPublicationWhenUnpublished() {
        MediaAssetEntity asset = newAsset();
        asset.publish(NOW);
        asset.setCover(true, NOW);

        asset.unpublish(NOW);

        assertThat(asset.getPublishedAt()).isNull();
        assertThat(asset.isCover()).isFalse();
    }

    @Test
    void shouldIrreversiblyMarkArchivedAndClearPublication() {
        MediaAssetEntity asset = newAsset();
        asset.publish(NOW);
        asset.setCover(true, NOW);

        asset.archive(NOW);

        assertThat(asset.getArchivedAt()).isEqualTo(NOW);
        assertThat(asset.getPublishedAt()).isNull();
        assertThat(asset.isCover()).isFalse();
        assertThat(asset.toDomain().archived()).isTrue();
        assertThat(asset.toDomain().published()).isFalse();
    }
}
