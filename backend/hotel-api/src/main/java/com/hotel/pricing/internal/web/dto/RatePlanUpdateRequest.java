package com.hotel.pricing.internal.web.dto;

import jakarta.validation.constraints.Size;

public record RatePlanUpdateRequest(
        @Size(max = 120) String nameTr,
        @Size(max = 120) String nameEn,
        Boolean active) {}
