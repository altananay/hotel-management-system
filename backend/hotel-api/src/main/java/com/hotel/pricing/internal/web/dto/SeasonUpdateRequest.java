package com.hotel.pricing.internal.web.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SeasonUpdateRequest(
        @Size(max = 120) String nameTr,
        @Size(max = 120) String nameEn,
        LocalDate startDate,
        LocalDate endDate) {}
