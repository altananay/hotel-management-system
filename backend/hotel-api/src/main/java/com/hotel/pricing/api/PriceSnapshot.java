package com.hotel.pricing.api;

import com.hotel.sharedkernel.api.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable price snapshot payload produced by pricing at reservation time. Persisting the
 * snapshot is the booking module's responsibility; pricing never stores snapshots.
 */
public record PriceSnapshot(
        UUID roomTypeId,
        String ratePlanCode,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        SupportedCurrency currency,
        List<PricedNight> nights,
        Money total,
        Instant pricedAt) {

    public PriceSnapshot {
        Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
        Objects.requireNonNull(ratePlanCode, "ratePlanCode must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        nights = List.copyOf(Objects.requireNonNull(nights, "nights must not be null"));
        Objects.requireNonNull(total, "total must not be null");
        Objects.requireNonNull(pricedAt, "pricedAt must not be null");
    }

    public int nightCount() {
        return nights.size();
    }
}
