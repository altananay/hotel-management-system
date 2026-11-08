package com.hotel.pricing.internal.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record PricingSettings(
        BigDecimal bufferPercent, BigDecimal roundingStep, Instant updatedAt) {

    public PricingSettings {
        bufferPercent = PriceRules.requireBuffer(bufferPercent);
        roundingStep = PriceRules.requireRoundingStep(roundingStep);
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
