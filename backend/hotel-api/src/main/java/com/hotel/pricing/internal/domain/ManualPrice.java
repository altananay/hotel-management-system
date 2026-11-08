package com.hotel.pricing.internal.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record ManualPrice(
        UUID id,
        UUID ratePlanId,
        UUID roomTypeId,
        int guests,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal amountTry,
        Instant createdAt,
        Instant updatedAt) {

    public ManualPrice {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(ratePlanId, "ratePlanId must not be null");
        Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
        PriceRules.requireGuests(guests);
        PriceRules.requirePeriod(startDate, endDate);
        amountTry = PriceRules.requirePositiveAmount(amountTry);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ManualPrice create(
            UUID id,
            UUID ratePlanId,
            UUID roomTypeId,
            int guests,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal amountTry,
            Instant now) {
        return new ManualPrice(
                id, ratePlanId, roomTypeId, guests, startDate, endDate, amountTry, now, now);
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
