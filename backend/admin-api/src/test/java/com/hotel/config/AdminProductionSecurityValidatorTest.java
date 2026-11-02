package com.hotel.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdminProductionSecurityValidatorTest {

    @Test
    void shouldAcceptProdConfiguration_whenCookieIsSecureAndSecretIsCustom() {
        assertThatCode(
                        () ->
                                new AdminProductionSecurityValidator(
                                        true, "prod-csrf-secret-with-at-least-32-bytes!"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInsecureCookie_whenOutsideLocalAndCiProfiles() {
        assertThatThrownBy(
                        () ->
                                new AdminProductionSecurityValidator(
                                        false, "prod-csrf-secret-with-at-least-32-bytes!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_COOKIE_SECURE");
    }

    @Test
    void shouldRejectLocalDefaultSecret_whenOutsideLocalAndCiProfiles() {
        assertThatThrownBy(
                        () ->
                                new AdminProductionSecurityValidator(
                                        true,
                                        AdminProductionSecurityValidator
                                                .LOCAL_DEFAULT_CSRF_SECRET))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_CSRF_SECRET");
    }

    @Test
    void shouldRejectShortSecret_whenOutsideLocalAndCiProfiles() {
        assertThatThrownBy(
                        () -> new AdminProductionSecurityValidator(true, "too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_CSRF_SECRET");
    }
}
