package com.hotel.pricing.api;

import java.time.LocalDate;

public record QuoteRequest(
        java.util.UUID roomTypeId,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        String currency) {}
