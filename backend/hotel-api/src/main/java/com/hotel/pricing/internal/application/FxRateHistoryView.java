package com.hotel.pricing.internal.application;

import com.hotel.pricing.internal.domain.FxRate;
import java.util.List;

public record FxRateHistoryView(
        List<FxRate> latest,
        List<FxRate> items,
        int page,
        int size,
        long total,
        boolean stale,
        List<String> staleCurrencies) {

    public FxRateHistoryView {
        latest = List.copyOf(latest);
        items = List.copyOf(items);
        staleCurrencies = List.copyOf(staleCurrencies);
    }
}
