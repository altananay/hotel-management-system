package com.hotel.pricing.internal.application;

import com.hotel.catalog.api.RoomTypeSnapshot;
import com.hotel.pricing.internal.domain.ManualPrice;

public record ManualPriceAdminView(ManualPrice manualPrice, RoomTypeSnapshot roomType) {}
