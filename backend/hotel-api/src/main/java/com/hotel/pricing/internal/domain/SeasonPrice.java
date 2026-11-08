package com.hotel.pricing.internal.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SeasonPrice(
        UUID id,
        UUID seasonId,
        UUID roomTypeId,
        int guests,
        BigDecimal amountTry,
        Instant createdAt,
        Instant updatedAt) {

    public SeasonPrice {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(seasonId, "seasonId must not be null");
        Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
        PriceRules.requireGuests(guests);
        amountTry = PriceRules.requirePositiveAmount(amountTry);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static SeasonPrice create(
            UUID id,
            UUID seasonId,
            UUID roomTypeId,
            int guests,
            BigDecimal amountTry,
            Instant now) {
        return new SeasonPrice(id, seasonId, roomTypeId, guests, amountTry, now, now);
    }

    public SeasonPrice withAmount(BigDecimal newAmount, Instant now) {
        return new SeasonPrice(id, seasonId, roomTypeId, guests, newAmount, createdAt, now);
    }
}
