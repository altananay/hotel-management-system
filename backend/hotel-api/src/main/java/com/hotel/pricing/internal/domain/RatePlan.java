package com.hotel.pricing.internal.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RatePlan(
        UUID id,
        String code,
        String nameTr,
        String nameEn,
        boolean active,
        boolean defaultPlan,
        Instant createdAt,
        Instant updatedAt) {

    public RatePlan {
        Objects.requireNonNull(id, "id must not be null");
        code = PriceRules.requireCode(code);
        nameTr = PriceRules.requireName(nameTr, "Turkish name");
        nameEn = PriceRules.requireName(nameEn, "English name");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static RatePlan create(
            UUID id,
            String code,
            String nameTr,
            String nameEn,
            boolean active,
            Instant createdAt) {
        return new RatePlan(id, code, nameTr, nameEn, active, false, createdAt, createdAt);
    }

    public RatePlan withNames(String newNameTr, String newNameEn, Instant now) {
        return new RatePlan(
                id,
                code,
                newNameTr,
                newNameEn,
                active,
                defaultPlan,
                createdAt,
                Objects.requireNonNull(now, "now must not be null"));
    }

    public RatePlan withActive(boolean newActive, Instant now) {
        return new RatePlan(
                id, code, nameTr, nameEn, newActive, defaultPlan, createdAt, now);
    }

    public RatePlan withDefault(boolean newDefault, Instant now) {
        return new RatePlan(id, code, nameTr, nameEn, active, newDefault, createdAt, now);
    }
}
