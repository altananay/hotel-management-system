package com.hotel.pricing.internal.domain;

import com.hotel.pricing.api.FxSource;
import com.hotel.pricing.api.SupportedCurrency;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record FxRate(
        UUID id,
        SupportedCurrency currency,
        FxSource source,
        LocalDate rateDate,
        BigDecimal rate,
        Instant capturedAt,
        Instant createdAt,
        Instant updatedAt) {

    public FxRate {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (currency == SupportedCurrency.TRY) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_CURRENCY,
                    "TRY is the base currency and does not have an FX rate.",
                    DomainErrorCategory.VALIDATION);
        }
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(rateDate, "rateDate must not be null");
        rate = PriceRules.requireFxRate(rate);
        Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static FxRate create(
            UUID id,
            SupportedCurrency currency,
            FxSource source,
            LocalDate rateDate,
            BigDecimal rate,
            Instant capturedAt,
            Instant now) {
        return new FxRate(id, currency, source, rateDate, rate, capturedAt, now, now);
    }

    public boolean isStale(LocalDate today) {
        return rateDate.isBefore(today);
    }
}
