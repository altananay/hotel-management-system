package com.hotel.pricing.internal.web.dto;

import java.util.UUID;

public record SeasonResponse(
        UUID id,
        UUID ratePlanId,
        String code,
        String nameTr,
        String nameEn,
        String startDate,
        String endDate,
        String createdAt,
        String updatedAt) {}
