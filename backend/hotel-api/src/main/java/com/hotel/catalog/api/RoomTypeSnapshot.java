package com.hotel.catalog.api;

/**
 * Read-only room type snapshot used by modules that need active or inactive room type names
 * without importing catalog internals. Pricing uses it to render existing prices for room types
 * that have since been deactivated.
 */
public record RoomTypeSnapshot(
        RoomTypeId id, String nameTr, String nameEn, int maxGuests, boolean active) {}
