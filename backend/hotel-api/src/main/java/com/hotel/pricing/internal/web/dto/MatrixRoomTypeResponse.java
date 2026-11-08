package com.hotel.pricing.internal.web.dto;

import java.util.UUID;

public record MatrixRoomTypeResponse(
        UUID id, String nameTr, String nameEn, int maxGuests, boolean active) {}
