package com.hotel.pricing.internal.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SettingsUpdateRequest(
        @NotNull BigDecimal bufferPercent, @NotNull BigDecimal roundingStep) {}
