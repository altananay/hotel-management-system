package com.hotel.pricing.api;

import java.util.UUID;

public record MissingPrice(UUID roomTypeId, int guests) {}
