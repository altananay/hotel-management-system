package com.hotel.pricing.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ManualFxRateCreateRequest(
        @NotBlank String currency,
        @NotNull LocalDate rateDate,
        @NotNull BigDecimal rate) {}
