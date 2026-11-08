package com.hotel.pricing.internal.web.dto;

import java.util.UUID;

public record ManualPriceResponse(
        UUID id,
        UUID ratePlanId,
        UUID roomTypeId,
        String roomTypeNameTr,
        String roomTypeNameEn,
        int guests,
        String startDate,
        String endDate,
        String amountTry,
        String createdAt,
        String updatedAt) {}
