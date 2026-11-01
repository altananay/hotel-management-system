package com.hotel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;

class AdminApiApplicationTest {

    @Test
    void shouldUseBootstrapMode_whenBootstrapFlagIsPresent() {
        assertThat(AdminApiApplication.isBootstrapMode(new String[] {"--bootstrap-admin"}))
                .isTrue();
        assertThat(AdminApiApplication.isBootstrapMode(new String[] {}))
                .isFalse();
    }

    @Test
    void shouldStartWithoutWebServer_whenBootstrapFlagIsPresent() {
        assertThat(
                        AdminApiApplication.webApplicationType(
                                new String[] {"--bootstrap-admin", "--spring.main.banner-mode=off"}))
                .isEqualTo(WebApplicationType.NONE);
        assertThat(AdminApiApplication.webApplicationType(new String[] {}))
                .isEqualTo(WebApplicationType.SERVLET);
    }
}
