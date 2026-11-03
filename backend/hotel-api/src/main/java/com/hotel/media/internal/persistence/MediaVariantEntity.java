package com.hotel.media.internal.persistence;

import com.hotel.media.internal.domain.MediaVariant;
import com.hotel.media.internal.domain.MediaVariantFormat;
import com.hotel.media.internal.domain.MediaVariantType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "media", name = "media_variants")
public class MediaVariantEntity {

    @Id private UUID id;

    @Column(name = "media_asset_id", nullable = false)
    private UUID mediaAssetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaVariantType variant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaVariantFormat format;

    @Column(name = "private_storage_key", nullable = false, unique = true, length = 512)
    private String privateStorageKey;

    @Column(name = "public_storage_key", unique = true, length = 512)
    private String publicStorageKey;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MediaVariantEntity() {}

    public MediaVariantEntity(
            UUID id,
            UUID mediaAssetId,
            MediaVariantType variant,
            MediaVariantFormat format,
            String privateStorageKey,
            int width,
            int height,
            long sizeBytes,
            Instant now) {
        this.id = id;
        this.mediaAssetId = mediaAssetId;
        this.variant = variant;
        this.format = format;
        this.privateStorageKey = privateStorageKey;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.createdAt = now;
    }

    public void publish(String publicStorageKey) {
        this.publicStorageKey = publicStorageKey;
    }

    public void unpublish() {
        this.publicStorageKey = null;
    }

    public MediaVariant toDomain() {
        return new MediaVariant(
                id,
                mediaAssetId,
                variant,
                format,
                privateStorageKey,
                publicStorageKey,
                width,
                height,
                sizeBytes);
    }

    public UUID getId() {
        return id;
    }

    public UUID getMediaAssetId() {
        return mediaAssetId;
    }

    public MediaVariantType getVariant() {
        return variant;
    }

    public MediaVariantFormat getFormat() {
        return format;
    }

    public String getPrivateStorageKey() {
        return privateStorageKey;
    }

    public String getPublicStorageKey() {
        return publicStorageKey;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
