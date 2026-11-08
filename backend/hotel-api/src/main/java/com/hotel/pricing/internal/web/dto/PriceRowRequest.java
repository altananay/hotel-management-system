package com.hotel.pricing.internal.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PriceRowRequest(
        @NotNull UUID roomTypeId, int guests, @NotNull BigDecimal amountTry) {}
