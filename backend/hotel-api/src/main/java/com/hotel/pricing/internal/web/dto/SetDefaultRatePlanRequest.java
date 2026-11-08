package com.hotel.pricing.internal.web.dto;

import java.util.UUID;

public record SetDefaultRatePlanRequest(UUID expectedDefaultPlanId) {}
