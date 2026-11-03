package com.hotel.media.api;

import java.util.UUID;

/** Published after a media asset and its public variants are ready. */
public record MediaReady(UUID mediaAssetId, UUID roomTypeId) {}
