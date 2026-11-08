package com.hotel.pricing.internal.application;

import com.hotel.pricing.internal.domain.RatePlan;
import com.hotel.pricing.internal.domain.Season;
import java.util.List;

public record RatePlanAdminView(RatePlan plan, List<Season> seasons) {

    public RatePlanAdminView {
        seasons = List.copyOf(seasons);
    }
}
