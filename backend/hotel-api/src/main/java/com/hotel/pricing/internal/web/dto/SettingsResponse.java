package com.hotel.pricing.internal.web.dto;

public record SettingsResponse(
        String bufferPercent, String roundingStep, String updatedAt) {}
