package com.hotel.adminidentity.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record AdminSession(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant createdAt,
        Instant lastSeenAt,
        Instant absoluteExpiresAt) {}
