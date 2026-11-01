package com.hotel.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.RemoteKeySourceException;
import java.net.ConnectException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtException;
import tools.jackson.databind.ObjectMapper;

class AdminJwtAuthenticationEntryPointTest {

    private final AdminJwtAuthenticationEntryPoint entryPoint =
            new AdminJwtAuthenticationEntryPoint(new ObjectMapper());

    @Test
    void shouldReturn503_whenJwksRetrievalFails() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/admin-api/rooms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationServiceException failure =
                new AuthenticationServiceException(
                        "JWT decoding failed",
                        new JwtException(
                                "JWKS unavailable",
                                new RemoteKeySourceException(
                                        "connection refused", new ConnectException("refused"))));

        entryPoint.commence(request, response, failure);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType())
                .startsWith("application/problem+json");
        assertThat(response.getContentAsString()).contains("system.http_503");
        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isNotBlank();
    }

    @Test
    void shouldReturn401_whenTokenIsInvalid() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/admin-api/rooms");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request, response, new BadCredentialsException("invalid token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("security.unauthorized");
    }
}
