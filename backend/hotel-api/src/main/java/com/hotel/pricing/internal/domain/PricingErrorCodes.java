package com.hotel.pricing.internal.domain;

public final class PricingErrorCodes {

    public static final String INVALID_PERIOD = "pricing.invalid_period";
    public static final String INVALID_GUESTS = "pricing.invalid_guests";
    public static final String INVALID_CURRENCY = "pricing.invalid_currency";
    public static final String INVALID_AMOUNT = "pricing.invalid_amount";
    public static final String INVALID_BUFFER = "pricing.invalid_buffer";
    public static final String INVALID_ROUNDING_STEP = "pricing.invalid_rounding_step";
    public static final String INVALID_FX_RATE = "pricing.invalid_fx_rate";
    public static final String ROOM_TYPE_NOT_FOUND = "pricing.room_type_not_found";
    public static final String PRICE_NOT_CONFIGURED = "pricing.price_not_configured";
    public static final String DEFAULT_RATE_PLAN_REQUIRED = "pricing.default_rate_plan_required";
    public static final String DEFAULT_RATE_PLAN_INCOMPLETE =
            "pricing.default_rate_plan_incomplete";
    public static final String RATE_PLAN_NOT_FOUND = "pricing.rate_plan_not_found";
    public static final String RATE_PLAN_DUPLICATE = "pricing.rate_plan_duplicate";
    public static final String RATE_PLAN_IN_USE = "pricing.rate_plan_in_use";
    public static final String RATE_PLAN_PRICE_NOT_FOUND = "pricing.rate_plan_price_not_found";
    public static final String SEASON_NOT_FOUND = "pricing.season_not_found";
    public static final String SEASON_PRICE_NOT_FOUND = "pricing.season_price_not_found";
    public static final String SEASON_DUPLICATE = "pricing.season_duplicate";
    public static final String SEASON_OVERLAP = "pricing.season_overlap";
    public static final String MANUAL_PRICE_OVERLAP = "pricing.manual_price_overlap";
    public static final String MANUAL_PRICE_NOT_FOUND = "pricing.manual_price_not_found";
    public static final String MANUAL_FX_DUPLICATE = "pricing.manual_fx_duplicate";
    public static final String MANUAL_FX_NOT_FOUND = "pricing.manual_fx_not_found";
    public static final String FX_RATE_UNAVAILABLE = "pricing.fx_rate_unavailable";

    private PricingErrorCodes() {}
}
