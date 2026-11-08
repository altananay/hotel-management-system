package com.hotel.pricing.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Unit tests for the configurable maximum stay nights (CR-14). */
class PricingPropertiesTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(PropertiesConfig.class);

    @Test
    void shouldDefaultToThirtyNights() {
        assertThat(new PricingProperties().getMaxStayNights()).isEqualTo(30);
    }

    @Test
    void shouldBindConfiguredMaxStayNights() {
        runner.withPropertyValues("hotel.pricing.max-stay-nights=45")
                .run(
                        context ->
                                assertThat(
                                                context.getBean(PricingProperties.class)
                                                        .getMaxStayNights())
                                        .isEqualTo(45));
    }

    @Test
    void shouldRejectNonPositiveMaxStayNights() {
        assertThatThrownBy(() -> new PricingProperties().setMaxStayNights(0))
                .isInstanceOf(IllegalArgumentException.class);
        runner.withPropertyValues("hotel.pricing.max-stay-nights=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(PricingProperties.class)
    static class PropertiesConfig {}
}
