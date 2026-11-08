package com.hotel.pricing.internal.web.dto;

import java.util.UUID;

public record PriceRowResponse(
        UUID id, UUID roomTypeId, int guests, String amountTry, String updatedAt) {}
