package com.hotel.pricing.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotel.pricing.api.SupportedCurrency;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for quote calculation, source precedence and night-by-night rounding (AC-3, AC-4, AC-5). */
class QuoteCalculatorTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-08T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldPriceTryExactlyWithoutFxBufferOrRounding() {
        RatePlan plan =
                new RatePlan(
                        UUID.randomUUID(),
                        "STANDARD",
                        "Standart",
                        "Standard",
                        true,
                        true,
                        CLOCK.instant(),
                        CLOCK.instant());
        PricingSettings settings =
                new PricingSettings(
                        new BigDecimal("2.00"), new BigDecimal("5.00"), CLOCK.instant());
        List<PriceResolution> resolutions =
                List.of(
                        resolution("2026-11-10", "100.01"),
                        resolution("2026-11-11", "100.02"));
        var quote =
                QuoteCalculator.calculate(
                        UUID.randomUUID(),
                        plan,
                        LocalDate.parse("2026-11-10"),
                        LocalDate.parse("2026-11-12"),
                        2,
                        SupportedCurrency.TRY,
                        resolutions,
                        (currency, date) -> java.util.Optional.empty(),
                        settings,
                        CLOCK,
                        Duration.ofMinutes(15));
        assertThat(quote.total().amount()).isEqualByComparingTo("200.03");
        assertThat(quote.nights()).allSatisfy(night -> assertThat(night.fxRate()).isNull());
        assertThat(quote.nights()).allSatisfy(night -> assertThat(night.amount().amount()).isEqualByComparingTo(night.baseAmountTry().amount()));
    }

    @Test
    void shouldRoundEachNightAndSumWithoutReRounding() {
        RatePlan plan =
                new RatePlan(
                        UUID.randomUUID(),
                        "STANDARD",
                        "Standart",
                        "Standard",
                        true,
                        true,
                        CLOCK.instant(),
                        CLOCK.instant());
        PricingSettings settings =
                new PricingSettings(
                        new BigDecimal("2.00"), new BigDecimal("5.00"), CLOCK.instant());
        List<PriceResolution> resolutions =
                List.of(
                        resolution("2026-11-10", "100.01"),
                        resolution("2026-11-11", "100.01"));
        FxRate fxRate =
                FxRate.create(
                        UUID.randomUUID(),
                        SupportedCurrency.EUR,
                        com.hotel.pricing.api.FxSource.TCMB,
                        LocalDate.parse("2026-11-07"),
                        new BigDecimal("32.500000"),
                        CLOCK.instant(),
                        CLOCK.instant());
        var quote =
                QuoteCalculator.calculate(
                        UUID.randomUUID(),
                        plan,
                        LocalDate.parse("2026-11-10"),
                        LocalDate.parse("2026-11-12"),
                        2,
                        SupportedCurrency.EUR,
                        resolutions,
                        (currency, date) -> java.util.Optional.of(fxRate),
                        settings,
                        CLOCK,
                        Duration.ofMinutes(15));
        assertThat(quote.nights()).hasSize(2);
        assertThat(quote.nights().get(0).amount().amount()).isEqualByComparingTo("3320.00");
        assertThat(quote.nights().get(1).amount().amount()).isEqualByComparingTo("3320.00");
        assertThat(quote.total().amount()).isEqualByComparingTo("6640.00");
        assertThat(quote.nights().get(0).fxStale()).isTrue();
    }

    private static PriceResolution resolution(String date, String amount) {
        return new PriceResolution(
                LocalDate.parse(date),
                null,
                null,
                null,
                com.hotel.pricing.api.PriceSource.BASE,
                new BigDecimal(amount));
    }
}
