package com.hotel.pricing.internal.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ManualPriceCreateRequest(
        @NotNull UUID ratePlanId,
        @NotNull UUID roomTypeId,
        int guests,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull BigDecimal amountTry) {}
