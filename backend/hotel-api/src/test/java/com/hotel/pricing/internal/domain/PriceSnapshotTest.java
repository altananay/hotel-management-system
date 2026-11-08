package com.hotel.pricing.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.pricing.api.PriceSource;
import com.hotel.pricing.api.PricedNight;
import com.hotel.pricing.api.PriceSnapshot;
import com.hotel.pricing.api.SupportedCurrency;
import com.hotel.sharedkernel.api.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for immutable price snapshots (AC-5). */
class PriceSnapshotTest {

    @Test
    void shouldCopyNightsAndRejectMutation() {
        List<PricedNight> nights = new ArrayList<>();
        nights.add(night());
        PriceSnapshot snapshot =
                new PriceSnapshot(
                        UUID.randomUUID(),
                        "STANDARD",
                        LocalDate.parse("2026-11-10"),
                        LocalDate.parse("2026-11-11"),
                        2,
                        SupportedCurrency.TRY,
                        nights,
                        Money.of(new BigDecimal("100.00"), "TRY"),
                        Instant.parse("2026-11-08T10:00:00Z"));
        nights.clear();
        assertThat(snapshot.nights()).hasSize(1);
        assertThatThrownBy(() -> snapshot.nights().add(night()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldBeDeterministicForSameInputs() {
        Instant now = Instant.parse("2026-11-08T10:00:00Z");
        PriceSnapshot first = snapshot(now);
        PriceSnapshot second = snapshot(now);
        assertThat(first).isEqualTo(second);
    }

    private static PriceSnapshot snapshot(Instant now) {
        return new PriceSnapshot(
                UUID.fromString("0199f2c0-0000-7000-8000-000000000701"),
                "STANDARD",
                LocalDate.parse("2026-11-10"),
                LocalDate.parse("2026-11-11"),
                2,
                SupportedCurrency.TRY,
                List.of(night()),
                Money.of(new BigDecimal("100.00"), "TRY"),
                now);
    }

    private static PricedNight night() {
        return new PricedNight(
                LocalDate.parse("2026-11-10"),
                null,
                null,
                null,
                PriceSource.BASE,
                Money.of(new BigDecimal("100.00"), "TRY"),
                null,
                null,
                null,
                false,
                null,
                null,
                Money.of(new BigDecimal("100.00"), "TRY"));
    }
}
