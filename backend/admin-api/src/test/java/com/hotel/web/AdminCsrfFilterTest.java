package com.hotel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hotel.adminidentity.api.AdminCsrfException;
import com.hotel.adminidentity.api.AdminIdentityApi;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AdminCsrfFilterTest {

    private AdminIdentityApi identityApi;
    private AdminProblemWriter problemWriter;
    private AdminCsrfFilter filter;

    @BeforeEach
    void setUp() {
        identityApi = mock(AdminIdentityApi.class);
        problemWriter = mock(AdminProblemWriter.class);
        filter = new AdminCsrfFilter(identityApi, problemWriter);
    }

    @Test
    void shouldReturn403AndSkipChain_whenCsrfValidationFails() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/admin-api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doThrow(new AdminCsrfException())
                .when(identityApi)
                .validateCsrf(any(), any(), any(), anyString(), eq(true));

        filter.doFilter(request, response, chain);

        verify(problemWriter)
                .write(
                        eq(request),
                        eq(response),
                        eq(HttpStatus.FORBIDDEN),
                        eq("security.csrf_invalid"),
                        anyString());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldPassThrough_whenCsrfValidationSucceeds() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/admin-api/auth/logout");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(problemWriter, never())
                .write(any(), any(), any(), anyString(), anyString());
    }

    @Test
    void shouldSkipSafeMethodsAndTokenEndpoint() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/admin-api/auth/me")))
                .isTrue();
        assertThat(
                        filter.shouldNotFilter(
                                new MockHttpServletRequest("GET", "/admin-api/auth/csrf")))
                .isTrue();
        assertThat(
                        filter.shouldNotFilter(
                                new MockHttpServletRequest("POST", "/admin-api/auth/csrf")))
                .isTrue();
        assertThat(
                        filter.shouldNotFilter(
                                new MockHttpServletRequest("POST", "/admin-api/auth/logout")))
                .isFalse();
    }
}
