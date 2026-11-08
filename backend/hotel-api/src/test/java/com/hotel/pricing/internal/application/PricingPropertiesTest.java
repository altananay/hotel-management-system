package com.hotel.pricing.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Unit tests for the configurable maximum stay nights (CR-14). */
class PricingPropertiesTest {

    @Test
    void shouldDefaultToThirtyNights() {
        assertThat(new PricingProperties().getMaxStayNights()).isEqualTo(30);
    }

    @Test
    void shouldAcceptPositiveOverride() {
        PricingProperties properties = new PricingProperties();
        properties.setMaxStayNights(45);
        assertThat(properties.getMaxStayNights()).isEqualTo(45);
    }

    @Test
    void shouldRejectNonPositiveMaxStayNights() {
        PricingProperties properties = new PricingProperties();
        assertThatThrownBy(() -> properties.setMaxStayNights(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
