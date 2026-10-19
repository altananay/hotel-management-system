package com.hotel.sharedkernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.StayPeriod;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StayPeriod} (AC-4).
 */
class StayPeriodTest {

    @Test
    void shouldCalculateNightsAndAllowPastOrLongFutureRanges_whenCheckOutIsAfterCheckIn() {
        StayPeriod pastStay =
                StayPeriod.of(LocalDate.of(2020, 1, 10), LocalDate.of(2020, 1, 13));
        assertThat(pastStay.nights()).isEqualTo(3);

        StayPeriod longStay =
                StayPeriod.of(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));
        assertThat(longStay.nights()).isEqualTo(365);
    }

    @Test
    void shouldThrowInvalidStayPeriodDomainError_whenCheckOutEqualsOrPrecedesCheckIn() {
        LocalDate date = LocalDate.of(2026, 10, 18);

        assertThatThrownBy(() -> StayPeriod.of(date, date))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(StayPeriod.INVALID_STAY_PERIOD_CODE);

        assertThatThrownBy(() -> StayPeriod.of(date, date.minusDays(1)))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(StayPeriod.INVALID_STAY_PERIOD_CODE);
    }
}
