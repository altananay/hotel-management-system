package com.hotel.pricing.internal.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class QuoteValidity {

    private QuoteValidity() {}

    public static Instant expiresAt(Instant quotedAt, Duration ttl) {
        Objects.requireNonNull(quotedAt, "quotedAt must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Quote TTL must be positive");
        }
        return quotedAt.plus(ttl);
    }

    public static boolean isExpired(Instant expiresAt, Instant now) {
        return !Objects.requireNonNull(now, "now must not be null").isBefore(expiresAt);
    }
}
