package com.hotel.pricing.internal.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ManualPriceUpdateRequest(
        UUID roomTypeId,
        @Min(1) Integer guests,
        LocalDate startDate,
        LocalDate endDate,
        @DecimalMin(value = "0.01") BigDecimal amountTry) {}
