package com.hotel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminApiApplicationTest {

    @Test
    void shouldUseBootstrapMode_whenBootstrapFlagIsPresent() {
        assertThat(AdminApiApplication.isBootstrapMode(new String[] {"--bootstrap-admin"}))
                .isTrue();
        assertThat(AdminApiApplication.isBootstrapMode(new String[] {}))
                .isFalse();
    }
}
