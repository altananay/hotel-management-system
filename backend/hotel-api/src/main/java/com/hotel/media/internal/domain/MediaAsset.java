package com.hotel.media.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record MediaAsset(
        UUID id,
        MediaType type,
        MediaStatus status,
        String storageKey,
        String mimeType,
        long sizeBytes,
        Integer width,
        Integer height,
        UUID roomTypeId,
        Instant publishedAt,
        Instant archivedAt,
        int displayOrder,
        boolean cover,
        String altTextTr,
        String altTextEn,
        String failureCode,
        int processingAttempts,
        Instant processingStartedAt,
        Instant createdAt,
        Instant updatedAt) {

    public boolean published() {
        return publishedAt != null;
    }

    public boolean archived() {
        return archivedAt != null;
    }
}
