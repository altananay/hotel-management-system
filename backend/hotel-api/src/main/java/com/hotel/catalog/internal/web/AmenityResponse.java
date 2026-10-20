package com.hotel.catalog.internal.web;

import com.hotel.catalog.internal.domain.Amenity;

public record AmenityResponse(String code, String nameTr, String nameEn) {

    public static AmenityResponse fromDomain(Amenity amenity) {
        return new AmenityResponse(amenity.code(), amenity.nameTr(), amenity.nameEn());
    }
}
