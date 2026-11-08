package com.hotel.pricing.internal.web.dto;

import java.util.List;
import java.util.UUID;

public record QuoteNightResponse(
        String date,
        String seasonCode,
        String seasonNameTr,
        String seasonNameEn,
        String priceSource,
        String baseAmountTry,
        String fxRate,
        String fxSource,
        String fxRateDate,
        Boolean fxStale,
        String bufferPercent,
        String roundingStep,
        String amount) {}
