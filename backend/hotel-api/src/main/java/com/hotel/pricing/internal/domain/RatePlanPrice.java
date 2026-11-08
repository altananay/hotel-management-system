package com.hotel.pricing.internal.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RatePlanPrice(
        UUID id,
        UUID ratePlanId,
        UUID roomTypeId,
        int guests,
        BigDecimal amountTry,
        Instant createdAt,
        Instant updatedAt) {

    public RatePlanPrice {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(ratePlanId, "ratePlanId must not be null");
        Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
        PriceRules.requireGuests(guests);
        amountTry = PriceRules.requirePositiveAmount(amountTry);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static RatePlanPrice create(
            UUID id,
            UUID ratePlanId,
            UUID roomTypeId,
            int guests,
            BigDecimal amountTry,
            Instant now) {
        return new RatePlanPrice(id, ratePlanId, roomTypeId, guests, amountTry, now, now);
    }

    public RatePlanPrice withAmount(BigDecimal newAmount, Instant now) {
        return new RatePlanPrice(
                id, ratePlanId, roomTypeId, guests, newAmount, createdAt, now);
    }
}
