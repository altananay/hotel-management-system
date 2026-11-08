package com.hotel.pricing.api;

import com.hotel.sharedkernel.api.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record PricedNight(
        LocalDate date,
        String seasonCode,
        String seasonNameTr,
        String seasonNameEn,
        PriceSource priceSource,
        Money baseAmountTry,
        BigDecimal fxRate,
        FxSource fxSource,
        LocalDate fxRateDate,
        boolean fxStale,
        BigDecimal bufferPercent,
        BigDecimal roundingStep,
        Money amount) {

    public PricedNight {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(priceSource, "priceSource must not be null");
        Objects.requireNonNull(baseAmountTry, "baseAmountTry must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
