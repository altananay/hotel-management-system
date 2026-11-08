package com.hotel.pricing.internal.domain;

import java.time.LocalDate;
import java.util.Optional;
import com.hotel.pricing.api.SupportedCurrency;

@FunctionalInterface
public interface FxLookup {
    Optional<FxRate> find(SupportedCurrency currency, LocalDate date);
}
