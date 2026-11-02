package com.hotel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hotel.adminidentity.api.AdminIdentityApi;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminClientContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AdminClientContextFilterTest {

    private AdminClientContextFilter filter;

    @BeforeEach
    void setUp() {
        filter =
                new AdminClientContextFilter(
                        new AdminClientContextFactory(new ClientIpResolver("")));
    }

    @Test
    void shouldSetLoginClientContextBeforeTheChain() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/admin-api/auth/login");
        request.setRemoteAddr("203.0.113.9");
        request.addHeader("User-Agent", "JUnit");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Object attribute = request.getAttribute(AdminIdentityApi.CLIENT_CONTEXT_ATTRIBUTE);
        assertThat(attribute).isInstanceOf(AdminClientContext.class);
        AdminClientContext context = (AdminClientContext) attribute;
        assertThat(context.ipAddress()).isEqualTo("203.0.113.9");
        assertThat(context.userAgent()).isEqualTo("JUnit");
        assertThat(context.requestId()).isNotBlank();
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNotDecorateNonAdminRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rooms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(AdminIdentityApi.CLIENT_CONTEXT_ATTRIBUTE)).isNull();
        verify(chain).doFilter(request, response);
    }
}
