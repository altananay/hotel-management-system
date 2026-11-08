package com.hotel.pricing.internal.web.dto;

import java.util.List;
import java.util.UUID;

public record RatePlanResponse(
        UUID id,
        String code,
        String nameTr,
        String nameEn,
        boolean active,
        boolean defaultPlan,
        String createdAt,
        String updatedAt,
        List<SeasonResponse> seasons) {}
