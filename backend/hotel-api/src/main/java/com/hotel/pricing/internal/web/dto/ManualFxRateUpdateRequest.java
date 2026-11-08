package com.hotel.pricing.internal.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ManualFxRateUpdateRequest(@NotNull BigDecimal rate) {}
