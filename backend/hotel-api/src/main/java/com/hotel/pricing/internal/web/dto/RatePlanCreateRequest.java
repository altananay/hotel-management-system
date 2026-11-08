package com.hotel.pricing.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RatePlanCreateRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 120) String nameTr,
        @NotBlank @Size(max = 120) String nameEn,
        Boolean active) {}
