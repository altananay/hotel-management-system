package com.hotel.catalog.internal.domain;

public final class CatalogErrorCodes {

    public static final String ROOM_TYPE_NAME_REQUIRED = "catalog.room_type_name_required";
    public static final String ROOM_TYPE_NAME_INVALID = "catalog.room_type_name_invalid";
    public static final String ROOM_TYPE_DESCRIPTION_REQUIRED =
            "catalog.room_type_description_required";
    public static final String ROOM_TYPE_DESCRIPTION_INVALID =
            "catalog.room_type_description_invalid";
    public static final String ROOM_TYPE_INVALID_MAX_GUESTS =
            "catalog.room_type_invalid_max_guests";
    public static final String ROOM_TYPE_INVALID_DISPLAY_ORDER =
            "catalog.room_type_invalid_display_order";

    public static final String AMENITY_NAME_REQUIRED = "catalog.amenity_name_required";
    public static final String AMENITY_NAME_INVALID = "catalog.amenity_name_invalid";
    public static final String AMENITY_CODE_INVALID = "catalog.amenity_code_invalid";
    public static final String AMENITY_CODE_DUPLICATE = "catalog.amenity_code_duplicate";

    private CatalogErrorCodes() {}
}
