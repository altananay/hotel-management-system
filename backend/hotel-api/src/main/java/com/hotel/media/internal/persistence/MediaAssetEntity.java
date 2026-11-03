package com.hotel.media.internal.persistence;

import com.hotel.media.internal.domain.MediaAsset;
import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.domain.MediaType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "media", name = "media_assets")
public class MediaAssetEntity {

    @Id private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaStatus status;

    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    private String storageKey;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column private Integer width;

    @Column private Integer height;

    @Column(name = "room_type_id")
    private UUID roomTypeId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_cover", nullable = false)
    private boolean cover;

    @Column(name = "alt_text_tr", length = 200)
    private String altTextTr;

    @Column(name = "alt_text_en", length = 200)
    private String altTextEn;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "processing_attempts", nullable = false)
    private short processingAttempts;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MediaAssetEntity() {}

    public MediaAssetEntity(
            UUID id,
            MediaType type,
            String storageKey,
            String mimeType,
            long sizeBytes,
            Instant now) {
        this.id = id;
        this.type = type;
        this.status = MediaStatus.UPLOADED;
        this.storageKey = storageKey;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.displayOrder = 0;
        this.processingAttempts = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markProcessing(
            String actualMimeType,
            long actualSize,
            int actualWidth,
            int actualHeight,
            Instant now) {
        this.status = MediaStatus.PROCESSING;
        this.mimeType = actualMimeType;
        this.sizeBytes = actualSize;
        this.width = actualWidth;
        this.height = actualHeight;
        this.failureCode = null;
        this.processingAttempts = 0;
        this.processingStartedAt = null;
        this.updatedAt = now;
    }

    public void markReady(Instant now) {
        this.status = MediaStatus.READY;
        this.failureCode = null;
        this.processingStartedAt = null;
        this.updatedAt = now;
    }

    public void markFailed(String failureCode, Instant now) {
        this.status = MediaStatus.FAILED;
        this.failureCode = failureCode;
        this.processingStartedAt = null;
        this.updatedAt = now;
    }

    public void prepareRetry(Instant now) {
        this.status = MediaStatus.PROCESSING;
        this.failureCode = null;
        this.processingAttempts = 0;
        this.processingStartedAt = null;
        this.updatedAt = now;
    }

    public void assignRoomType(UUID roomTypeId, int order, Instant now) {
        this.roomTypeId = roomTypeId;
        this.displayOrder = order;
        this.updatedAt = now;
    }

    public void updateAltText(String altTextTr, String altTextEn, Instant now) {
        this.altTextTr = altTextTr;
        this.altTextEn = altTextEn;
        this.updatedAt = now;
    }

    public void setDisplayOrder(int displayOrder, Instant now) {
        this.displayOrder = displayOrder;
        this.updatedAt = now;
    }

    public void setCover(boolean cover, Instant now) {
        this.cover = cover;
        this.updatedAt = now;
    }

    public void publish(Instant now) {
        this.publishedAt = now;
        this.updatedAt = now;
    }

    public void unpublish(Instant now) {
        this.publishedAt = null;
        this.cover = false;
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        this.archivedAt = now;
        this.publishedAt = null;
        this.cover = false;
        this.updatedAt = now;
    }

    public MediaAsset toDomain() {
        return new MediaAsset(
                id,
                type,
                status,
                storageKey,
                mimeType,
                sizeBytes,
                width,
                height,
                roomTypeId,
                publishedAt,
                archivedAt,
                displayOrder,
                cover,
                altTextTr,
                altTextEn,
                failureCode,
                processingAttempts,
                processingStartedAt,
                createdAt,
                updatedAt);
    }

    public UUID getId() {
        return id;
    }

    public MediaType getType() {
        return type;
    }

    public MediaStatus getStatus() {
        return status;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public UUID getRoomTypeId() {
        return roomTypeId;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isCover() {
        return cover;
    }

    public String getAltTextTr() {
        return altTextTr;
    }

    public String getAltTextEn() {
        return altTextEn;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public short getProcessingAttempts() {
        return processingAttempts;
    }

    public Instant getProcessingStartedAt() {
        return processingStartedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
