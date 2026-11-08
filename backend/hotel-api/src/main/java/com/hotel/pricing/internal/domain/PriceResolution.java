package com.hotel.pricing.internal.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import com.hotel.pricing.api.PriceSource;

public record PriceResolution(
        LocalDate date,
        String seasonCode,
        String seasonNameTr,
        String seasonNameEn,
        PriceSource priceSource,
        BigDecimal baseAmountTry) {

    public PriceResolution {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(priceSource, "priceSource must not be null");
        baseAmountTry = PriceRules.requirePositiveAmount(baseAmountTry);
    }
}
