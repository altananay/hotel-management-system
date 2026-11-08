package com.hotel.pricing.api;

import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.util.List;

public class PricingDefaultIncompleteException extends DomainError {

    private final List<MissingPrice> missing;

    public PricingDefaultIncompleteException(List<MissingPrice> missing) {
        super(
                "pricing.default_rate_plan_incomplete",
                "The rate plan cannot be the default because base prices are missing for active "
                        + "room types and guest counts.",
                DomainErrorCategory.CONFLICT);
        this.missing = List.copyOf(missing);
    }

    public List<MissingPrice> missing() {
        return missing;
    }
}
