package com.hotel.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Unit tests for the production media-storage fail-fast guard (AC-2, D11). */
class MediaStorageSafetyValidatorTest {

    @Test
    void shouldRejectDevelopmentCredentialsOutsideLocalAndCi() {
        assertThatThrownBy(
                        () ->
                                new MediaStorageSafetyValidator(
                                        "minioadmin",
                                        "minioadmin_local_dev",
                                        "https://storage.example.com",
                                        "https://storage.example.com",
                                        "https://cdn.example.com/media",
                                        "hotel-media"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MEDIA_S3_ACCESS_KEY");
    }

    @Test
    void shouldRejectShortSecretAndLocalEndpoints() {
        assertThatThrownBy(
                        () ->
                                new MediaStorageSafetyValidator(
                                        "production-access",
                                        "short",
                                        "https://storage.example.com",
                                        "https://storage.example.com",
                                        "https://cdn.example.com/media",
                                        "hotel-media"))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(
                        () ->
                                new MediaStorageSafetyValidator(
                                        "production-access",
                                        "a-long-production-secret",
                                        "https://storage.example.com",
                                        "http://localhost:9000",
                                        "https://cdn.example.com/media",
                                        "hotel-media"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void shouldAcceptStrongHttpsConfiguration() {
        assertThatCode(
                        () ->
                                new MediaStorageSafetyValidator(
                                        "production-access",
                                        "a-long-production-secret",
                                        "https://storage.example.com",
                                        "https://storage.example.com",
                                        "https://cdn.example.com/media",
                                        "hotel-media"))
                .doesNotThrowAnyException();
    }
}
