package com.hotel.pricing.internal.domain;

import com.hotel.pricing.api.SupportedCurrency;
import java.time.LocalDate;
import java.util.Optional;

@FunctionalInterface
public interface FxLookup {
    Optional<FxRate> find(SupportedCurrency currency, LocalDate date);
}
