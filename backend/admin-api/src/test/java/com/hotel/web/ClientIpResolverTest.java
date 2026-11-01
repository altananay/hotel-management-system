package com.hotel.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    void shouldUseFirstUntrustedForwardedAddress_whenProxyIsTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");

        assertThat(new ClientIpResolver("10.0.0.0/8").resolve(request))
                .isEqualTo("203.0.113.9");
    }

    @Test
    void shouldFallBackToDirectAddress_whenForwardedAddressIsInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "not-an-ip, 10.0.0.1");

        assertThat(new ClientIpResolver("10.0.0.0/8").resolve(request))
                .isEqualTo("10.0.0.2");
    }

    @Test
    void shouldFallBackToDirectAddress_whenForwardedAddressHasAnInvalidPort() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "203.0.113.9:443");

        assertThat(new ClientIpResolver("10.0.0.0/8").resolve(request))
                .isEqualTo("10.0.0.2");
    }

    @Test
    void shouldIgnoreForwardedHeader_whenNoProxyIsTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.9");
        request.addHeader("X-Forwarded-For", "198.51.100.7");

        assertThat(new ClientIpResolver("").resolve(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void shouldUseDirectAddress_whenAllForwardedHopsAreTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "10.0.0.3, 10.0.0.1");

        assertThat(new ClientIpResolver("10.0.0.0/8").resolve(request))
                .isEqualTo("10.0.0.2");
    }

    @Test
    void shouldUseDirectAddress_whenForwardedHeaderIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");

        assertThat(new ClientIpResolver("10.0.0.0/8").resolve(request))
                .isEqualTo("10.0.0.2");
    }
}
