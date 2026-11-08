package com.hotel.pricing.internal.domain;

import java.math.BigDecimal;

public final class FxConverter {

    private FxConverter() {}

    public static BigDecimal convert(
            BigDecimal tryAmount,
            BigDecimal fxRate,
            BigDecimal bufferPercent,
            BigDecimal roundingStep) {
        BigDecimal factor = BigDecimal.ONE.add(bufferPercent.movePointLeft(2));
        BigDecimal raw = tryAmount.multiply(fxRate).multiply(factor);
        return RoundingPolicy.roundUpToStep(raw, roundingStep);
    }
}
