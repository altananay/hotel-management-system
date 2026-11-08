package com.hotel.pricing.internal.web.dto;

import java.util.List;
import java.util.UUID;

public record PriceMatrixResponse(
        List<MatrixRoomTypeResponse> roomTypes,
        List<PriceRowResponse> rows,
        List<MissingPriceResponse> missing) {}
