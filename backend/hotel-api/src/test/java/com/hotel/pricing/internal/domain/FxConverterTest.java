package com.hotel.pricing.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Unit tests for the FX conversion formula (AC-3, Q6=A). */
class FxConverterTest {

    @Test
    void shouldApplyBufferAndRoundUp_whenCurrencyIsEur() {
        assertThat(
                        FxConverter.convert(
                                new BigDecimal("100.00"),
                                new BigDecimal("32.500000"),
                                new BigDecimal("2.00"),
                                new BigDecimal("5.00")))
                .isEqualByComparingTo("3315.00");
        assertThat(
                        FxConverter.convert(
                                new BigDecimal("100.00"),
                                new BigDecimal("32.500000"),
                                BigDecimal.ZERO.setScale(2),
                                new BigDecimal("5.00")))
                .isEqualByComparingTo("3250.00");
        assertThat(
                        FxConverter.convert(
                                new BigDecimal("100.00"),
                                new BigDecimal("32.500000"),
                                new BigDecimal("100.00"),
                                new BigDecimal("5.00")))
                .isEqualByComparingTo("6500.00");
    }

    @Test
    void shouldRoundTheConvertedAmountUpToTheConfiguredStep() {
        assertThat(
                        FxConverter.convert(
                                new BigDecimal("101.00"),
                                new BigDecimal("32.500000"),
                                new BigDecimal("2.00"),
                                new BigDecimal("5.00")))
                .isEqualByComparingTo("3350.00");
    }
}
