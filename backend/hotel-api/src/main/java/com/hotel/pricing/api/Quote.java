package com.hotel.pricing.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import com.hotel.sharedkernel.api.Money;

public record Quote(
        UUID roomTypeId,
        String ratePlanCode,
        String ratePlanNameTr,
        String ratePlanNameEn,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        SupportedCurrency currency,
        List<PricedNight> nights,
        Money total,
        Instant quotedAt,
        Instant expiresAt) {

    public Quote {
        Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
        Objects.requireNonNull(ratePlanCode, "ratePlanCode must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        nights = List.copyOf(Objects.requireNonNull(nights, "nights must not be null"));
        Objects.requireNonNull(total, "total must not be null");
        Objects.requireNonNull(quotedAt, "quotedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    public int nightCount() {
        return nights.size();
    }

    public boolean isExpired(Instant now) {
        return !Objects.requireNonNull(now, "now must not be null").isBefore(expiresAt);
    }
}
