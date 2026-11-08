package com.hotel.pricing.internal.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ManualPriceUpdateRequest(
        UUID roomTypeId,
        Integer guests,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal amountTry) {}
