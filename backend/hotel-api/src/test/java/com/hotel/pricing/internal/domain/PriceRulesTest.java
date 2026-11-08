package com.hotel.pricing.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Unit tests for pricing validation rules (AC-1, AC-3). */
class PriceRulesTest {

    @Test
    void shouldNormalizeCodeAndTrimNames() {
        assertThat(PriceRules.requireCode(" standard ")).isEqualTo("STANDARD");
        assertThat(PriceRules.requireName("  Standart  ", "Turkish name")).isEqualTo("Standart");
    }

    @Test
    void shouldRejectInvalidCodesAndNames() {
        assertThatThrownBy(() -> PriceRules.requireCode("1BAD")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> PriceRules.requireName(" ", "Turkish name"))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> PriceRules.requireName("bad<name", "Turkish name"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldAcceptOnlyTwoDecimalAmounts() {
        assertThat(PriceRules.requirePositiveAmount(new BigDecimal("10.00")))
                .isEqualByComparingTo("10.00");
        assertThatThrownBy(() -> PriceRules.requirePositiveAmount(new BigDecimal("10.005")))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> PriceRules.requirePositiveAmount(BigDecimal.ZERO))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldValidateBufferRoundingStepAndFxRate() {
        assertThat(PriceRules.requireBuffer(new BigDecimal("2.00"))).isEqualByComparingTo("2.00");
        assertThat(PriceRules.requireBuffer(new BigDecimal("100.00")))
                .isEqualByComparingTo("100.00");
        assertThatThrownBy(() -> PriceRules.requireBuffer(new BigDecimal("100.01")))
                .isInstanceOf(RuntimeException.class);
        assertThat(PriceRules.requireRoundingStep(new BigDecimal("5.00")))
                .isEqualByComparingTo("5.00");
        assertThatThrownBy(() -> PriceRules.requireRoundingStep(BigDecimal.ZERO))
                .isInstanceOf(RuntimeException.class);
        assertThat(PriceRules.requireFxRate(new BigDecimal("42.123456")))
                .isEqualByComparingTo("42.123456");
        assertThatThrownBy(() -> PriceRules.requireFxRate(new BigDecimal("42.1234567")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldValidateGuestCountAndPeriod() {
        assertThat(PriceRules.requireGuests(1)).isEqualTo(1);
        assertThatThrownBy(() -> PriceRules.requireGuests(0)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(
                        () ->
                                PriceRules.requirePeriod(
                                        LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 9)))
                .isInstanceOf(RuntimeException.class);
    }
}
