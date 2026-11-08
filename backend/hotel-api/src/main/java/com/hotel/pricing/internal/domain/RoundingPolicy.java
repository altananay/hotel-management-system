package com.hotel.pricing.internal.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RoundingPolicy {

    private RoundingPolicy() {}

    /**
     * Rounds a positive amount up to the next multiple of the configured step. A value that is
     * already a multiple stays unchanged.
     */
    public static BigDecimal roundUpToStep(BigDecimal amount, BigDecimal step) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Amount must not be negative");
        }
        if (step == null || step.signum() <= 0) {
            throw new IllegalArgumentException("Rounding step must be positive");
        }
        BigDecimal quotient = amount.divide(step, 0, RoundingMode.CEILING);
        return quotient.multiply(step).setScale(2, RoundingMode.UNNECESSARY);
    }
}
