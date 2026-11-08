package com.hotel.pricing.api;

import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

public enum SupportedCurrency {
    TRY,
    EUR,
    USD;

    public static SupportedCurrency from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainError(
                    "pricing.invalid_currency",
                    "Currency must be one of TRY, EUR or USD.",
                    DomainErrorCategory.VALIDATION);
        }
        try {
            return SupportedCurrency.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new DomainError(
                    "pricing.invalid_currency",
                    "Currency must be one of TRY, EUR or USD.",
                    DomainErrorCategory.VALIDATION);
        }
    }

    public Currency javaCurrency() {
        return Objects.requireNonNull(Currency.getInstance(name()));
    }

    public BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
