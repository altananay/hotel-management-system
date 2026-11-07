package com.hotel.media.internal.web.dto;

import com.hotel.media.internal.application.MediaAdminService.AdminMediaView;
import com.hotel.media.internal.application.MediaStoragePort;
import com.hotel.media.internal.domain.MediaVariant;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MediaAdminItemResponse(
        UUID id,
        String type,
        String status,
        UUID roomTypeId,
        boolean published,
        boolean cover,
        int displayOrder,
        String altTextTr,
        String altTextEn,
        Original original,
        List<Variant> variants,
        String failureCode,
        Instant createdAt,
        Instant updatedAt) {

    public static MediaAdminItemResponse from(
            AdminMediaView view, MediaStoragePort storage) {
        var asset = view.asset();
        List<Variant> variants =
                view.variants().stream()
                        .map(variant -> Variant.from(variant, storage))
                        .toList();
        return new MediaAdminItemResponse(
                asset.id(),
                asset.type().name(),
                asset.status().name(),
                asset.roomTypeId(),
                asset.published(),
                asset.cover(),
                asset.displayOrder(),
                asset.altTextTr(),
                asset.altTextEn(),
                new Original(
                        asset.mimeType(),
                        asset.sizeBytes(),
                        asset.width(),
                        asset.height()),
                variants,
                asset.failureCode(),
                asset.createdAt(),
                asset.updatedAt());
    }

    public record Original(String mimeType, long sizeBytes, Integer width, Integer height) {}

    public record Variant(
            String variant, String format, String url, int width, int height) {

        static Variant from(MediaVariant variant, MediaStoragePort storage) {
            return new Variant(
                    variant.variant().name().toLowerCase(),
                    variant.format().name().toLowerCase(),
                    storage.presignGet(variant.privateStorageKey(), Duration.ofMinutes(5)),
                    variant.width(),
                    variant.height());
        }
    }
}
