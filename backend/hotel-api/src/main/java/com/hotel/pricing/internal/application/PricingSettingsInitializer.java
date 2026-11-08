package com.hotel.pricing.internal.application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PricingSettingsInitializer implements ApplicationRunner {

    private final PricingSettingsService settingsService;

    public PricingSettingsInitializer(PricingSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        settingsService.initializeIfAbsent();
    }
}
