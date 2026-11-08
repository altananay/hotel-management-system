package com.hotel.pricing.internal.domain;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import java.util.UUID;

public final class PricingIds {

    private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();

    private PricingIds() {}

    public static UUID newId() {
        return GENERATOR.generate();
    }
}
