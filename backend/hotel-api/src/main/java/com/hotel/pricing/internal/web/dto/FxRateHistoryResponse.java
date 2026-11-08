package com.hotel.pricing.internal.web.dto;

import java.util.List;

public record FxRateHistoryResponse(
        List<FxRateResponse> latest,
        List<FxRateResponse> items,
        int page,
        int size,
        long total,
        boolean stale,
        List<String> staleCurrencies) {}
