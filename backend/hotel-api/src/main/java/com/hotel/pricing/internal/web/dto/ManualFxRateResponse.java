package com.hotel.pricing.internal.web.dto;

import java.util.UUID;

public record ManualFxRateResponse(
        UUID id,
        String currency,
        String source,
        String rateDate,
        String rate,
        String capturedAt,
        String updatedAt) {}
