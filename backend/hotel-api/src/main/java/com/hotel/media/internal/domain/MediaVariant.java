package com.hotel.media.internal.domain;

import java.util.UUID;

public record MediaVariant(
        UUID id,
        UUID mediaAssetId,
        MediaVariantType variant,
        MediaVariantFormat format,
        String privateStorageKey,
        String publicStorageKey,
        int width,
        int height,
        long sizeBytes) {}
