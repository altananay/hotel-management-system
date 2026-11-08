package com.hotel.pricing.internal.web.dto;

import java.util.UUID;

public record MissingPriceResponse(UUID roomTypeId, int guests) {}
