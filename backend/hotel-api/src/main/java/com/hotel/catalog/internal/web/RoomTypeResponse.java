package com.hotel.catalog.internal.web;

import com.hotel.catalog.internal.domain.RoomType;
import java.util.List;

public record RoomTypeResponse(
        String id,
        String nameTr,
        String nameEn,
        String descriptionTr,
        String descriptionEn,
        int maxGuests,
        List<AmenityResponse> amenities) {

    public static RoomTypeResponse fromDomain(RoomType roomType) {
        List<AmenityResponse> amenityResponses =
                roomType.amenities().stream().map(AmenityResponse::fromDomain).toList();
        return new RoomTypeResponse(
                roomType.id().toString(),
                roomType.nameTr(),
                roomType.nameEn(),
                roomType.descriptionTr(),
                roomType.descriptionEn(),
                roomType.maxGuests(),
                amenityResponses);
    }
}
