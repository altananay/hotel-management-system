package com.hotel.pricing.internal.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PriceMatrixRequest(@NotEmpty List<@Valid PriceRowRequest> rows) {}
