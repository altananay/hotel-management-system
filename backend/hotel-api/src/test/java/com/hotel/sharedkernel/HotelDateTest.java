package com.hotel.sharedkernel;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotel.sharedkernel.api.HotelDate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HotelDate} (AC-4).
 */
class HotelDateTest {

    @Test
    void shouldResolveBusinessDayInIstanbulTimezone_whenUsingFixedClockNearMidnight() {
        // 2026-10-18T22:30:00Z is 2026-10-19T01:30:00+03:00 in Europe/Istanbul
        Clock utcClock = Clock.fixed(Instant.parse("2026-10-18T22:30:00Z"), ZoneOffset.UTC);

        HotelDate hotelDate = HotelDate.now(utcClock);

        assertThat(hotelDate.value()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(hotelDate.toString()).isEqualTo("2026-10-19");
    }

    @Test
    void shouldParseAndFormatIsoDate_whenCreatingFromLocalDateOrString() {
        HotelDate parsed = HotelDate.parse("2026-05-15");
        HotelDate constructed = HotelDate.of(2026, 5, 15);

        assertThat(parsed).isEqualTo(constructed);
        assertThat(parsed.toString()).isEqualTo("2026-05-15");
        assertThat(parsed.isBefore(HotelDate.of(2026, 5, 16))).isTrue();
        assertThat(parsed.isAfter(HotelDate.of(2026, 5, 14))).isTrue();
    }
}
