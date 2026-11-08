package com.hotel.pricing.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Unit tests for upward rounding (AC-3, Q6=A). */
class RoundingPolicyTest {

    @Test
    void shouldRoundUpToNextStep_whenAmountIsNotMultiple() {
        assertThat(RoundingPolicy.roundUpToStep(new BigDecimal("0.01"), new BigDecimal("5.00")))
                .isEqualByComparingTo("5.00");
        assertThat(RoundingPolicy.roundUpToStep(new BigDecimal("4.99"), new BigDecimal("5.00")))
                .isEqualByComparingTo("5.00");
        assertThat(RoundingPolicy.roundUpToStep(new BigDecimal("5.00"), new BigDecimal("5.00")))
                .isEqualByComparingTo("5.00");
        assertThat(RoundingPolicy.roundUpToStep(new BigDecimal("5.01"), new BigDecimal("5.00")))
                .isEqualByComparingTo("10.00");
    }

    @Test
    void shouldSupportDifferentPositiveStep_whenConfigured() {
        assertThat(RoundingPolicy.roundUpToStep(new BigDecimal("0.01"), new BigDecimal("0.25")))
                .isEqualByComparingTo("0.25");
        assertThat(RoundingPolicy.roundUpToStep(new BigDecimal("0.26"), new BigDecimal("0.25")))
                .isEqualByComparingTo("0.50");
    }

    @Test
    void shouldRejectNegativeAmountOrNonPositiveStep() {
        assertThatThrownBy(
                        () ->
                                RoundingPolicy.roundUpToStep(
                                        new BigDecimal("-1.00"), new BigDecimal("5.00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                RoundingPolicy.roundUpToStep(
                                        new BigDecimal("1.00"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
