package com.hotel.pricing.internal.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Season(
        UUID id,
        UUID ratePlanId,
        String code,
        String nameTr,
        String nameEn,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        Instant updatedAt) {

    public Season {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(ratePlanId, "ratePlanId must not be null");
        code = PriceRules.requireCode(code);
        nameTr = PriceRules.requireName(nameTr, "Turkish name");
        nameEn = PriceRules.requireName(nameEn, "English name");
        PriceRules.requirePeriod(startDate, endDate);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Season create(
            UUID id,
            UUID ratePlanId,
            String code,
            String nameTr,
            String nameEn,
            LocalDate startDate,
            LocalDate endDate,
            Instant createdAt) {
        return new Season(
                id,
                ratePlanId,
                code,
                nameTr,
                nameEn,
                startDate,
                endDate,
                createdAt,
                createdAt);
    }

    public Season withDetails(
            String newNameTr,
            String newNameEn,
            LocalDate newStartDate,
            LocalDate newEndDate,
            Instant now) {
        return new Season(
                id,
                ratePlanId,
                code,
                newNameTr,
                newNameEn,
                newStartDate,
                newEndDate,
                createdAt,
                now);
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
