package com.hotel.pricing.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Unit tests for 15-minute quote validity (AC-4, AC-5). */
class QuoteValidityTest {

    @Test
    void shouldExpireAfterFifteenMinutes() {
        Instant quotedAt = Instant.parse("2026-11-08T10:00:00Z");
        Instant expiresAt = QuoteValidity.expiresAt(quotedAt, Duration.ofMinutes(15));
        assertThat(expiresAt).isEqualTo(Instant.parse("2026-11-08T10:15:00Z"));
        assertThat(QuoteValidity.isExpired(expiresAt, Instant.parse("2026-11-08T10:14:59Z")))
                .isFalse();
        assertThat(QuoteValidity.isExpired(expiresAt, Instant.parse("2026-11-08T10:15:00Z")))
                .isTrue();
    }
}
