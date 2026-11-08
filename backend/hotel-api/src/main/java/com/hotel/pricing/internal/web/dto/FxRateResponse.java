package com.hotel.pricing.internal.web.dto;

import java.util.UUID;

public record FxRateResponse(
        UUID id,
        String currency,
        String source,
        String rateDate,
        String rate,
        String capturedAt,
        String updatedAt) {}
