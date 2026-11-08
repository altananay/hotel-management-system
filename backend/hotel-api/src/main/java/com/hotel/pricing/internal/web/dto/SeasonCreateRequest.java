package com.hotel.pricing.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SeasonCreateRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 120) String nameTr,
        @NotBlank @Size(max = 120) String nameEn,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {}
