package com.hotel.pricing.internal.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ManualPriceCreateRequest(
        @NotNull UUID ratePlanId,
        @NotNull UUID roomTypeId,
        @Min(1) int guests,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amountTry) {}
