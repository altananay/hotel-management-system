package com.hotel.pricing.internal.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;

public final class PriceRules {

    private static final int MAX_NAME_LENGTH = 120;
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,49}$");

    private PriceRules() {}

    public static String requireCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalid("Rate plan and season codes are required.");
        }
        String normalized = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw invalid(
                    "Code must start with a letter and contain only A-Z, 0-9, _ or - (2-50).");
        }
        return normalized;
    }

    public static String requireName(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw invalid(field + " is required.");
        }
        String normalized = raw.trim();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw invalid(field + " must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        for (int index = 0; index < normalized.length(); index++) {
            char value = normalized.charAt(index);
            if (value == '<' || value == '>' || Character.isISOControl(value)) {
                throw invalid(field + " contains an invalid character.");
            }
        }
        return normalized;
    }

    public static BigDecimal requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_AMOUNT,
                    "Amount must be greater than zero.",
                    DomainErrorCategory.VALIDATION);
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_AMOUNT,
                    "Amount must have at most two decimal places.",
                    DomainErrorCategory.VALIDATION);
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal requireBuffer(BigDecimal bufferPercent) {
        if (bufferPercent == null
                || bufferPercent.signum() < 0
                || bufferPercent.compareTo(BigDecimal.valueOf(100)) > 0
                || bufferPercent.stripTrailingZeros().scale() > 2) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_BUFFER,
                    "Buffer percent must be between 0 and 100 with at most two decimal places.",
                    DomainErrorCategory.VALIDATION);
        }
        return bufferPercent.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal requireRoundingStep(BigDecimal roundingStep) {
        if (roundingStep == null
                || roundingStep.signum() <= 0
                || roundingStep.stripTrailingZeros().scale() > 2) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_ROUNDING_STEP,
                    "Rounding step must be greater than zero with at most two decimal places.",
                    DomainErrorCategory.VALIDATION);
        }
        return roundingStep.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal requireFxRate(BigDecimal rate) {
        if (rate == null
                || rate.signum() <= 0
                || rate.stripTrailingZeros().scale() > 6) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_FX_RATE,
                    "FX rate must be greater than zero with at most six decimal places.",
                    DomainErrorCategory.VALIDATION);
        }
        return rate.setScale(6, RoundingMode.UNNECESSARY);
    }

    public static int requireGuests(int guests) {
        if (guests < 1) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_GUESTS,
                    "Guest count must be at least 1.",
                    DomainErrorCategory.VALIDATION);
        }
        return guests;
    }

    public static void requirePeriod(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        if (startDate.isAfter(endDate)) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_PERIOD,
                    "Start date must not be after end date.",
                    DomainErrorCategory.VALIDATION);
        }
    }

    public static DomainError invalid(String message) {
        return new DomainError("validation.invalid_request", message, DomainErrorCategory.VALIDATION);
    }
}
