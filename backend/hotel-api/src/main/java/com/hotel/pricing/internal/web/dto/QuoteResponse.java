package com.hotel.pricing.internal.web.dto;

import java.util.List;
import java.util.UUID;

public record QuoteResponse(
        UUID roomTypeId,
        String ratePlanCode,
        String ratePlanNameTr,
        String ratePlanNameEn,
        String checkIn,
        String checkOut,
        int guests,
        String currency,
        int nightCount,
        List<QuoteNightResponse> nights,
        String total,
        String quotedAt,
        String expiresAt) {}
