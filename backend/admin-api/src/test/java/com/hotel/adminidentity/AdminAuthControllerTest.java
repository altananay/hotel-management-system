package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotel.adminidentity.api.AdminIdentityApi;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminSessionView;
import com.hotel.adminidentity.api.AdminIdentityApi.CsrfToken;
import com.hotel.adminidentity.api.AdminSessionException;
import com.hotel.adminidentity.internal.web.AdminAuthController;
import com.hotel.adminidentity.internal.web.AdminCookieFactory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock private AdminIdentityApi identityApi;

    private AdminAuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminAuthController(identityApi, new AdminCookieFactory(false));
    }

    @Test
    void shouldIssuePreAuthCsrf_whenNoSessionCookieIsPresent() {
        when(identityApi.issuePreAuthCsrf()).thenReturn(new CsrfToken("pre-auth-token", 900));

        ResponseEntity<Void> response = controller.csrf(null, new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cookies(response))
                .singleElement()
                .satisfies(
                        cookie -> assertThat(cookie).startsWith("XSRF-TOKEN=pre-auth-token;"));
        verify(identityApi, never()).validateSession(any(), any());
    }

    @Test
    void shouldIssueSessionBoundCsrf_whenSessionCookieIsValid() {
        AdminSessionView session =
                new AdminSessionView(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "superadmin",
                        "superadmin@example.com",
                        "SUPER_ADMIN");
        when(identityApi.validateSession(eq("raw-session"), any())).thenReturn(session);
        when(identityApi.issueSessionCsrf(session))
                .thenReturn(new CsrfToken("session-token", 28_800));

        ResponseEntity<Void> response = controller.csrf("raw-session", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cookies(response))
                .singleElement()
                .satisfies(
                        cookie ->
                                assertThat(cookie)
                                        .startsWith("XSRF-TOKEN=session-token;")
                                        .contains("Max-Age=28800"));
        verify(identityApi, never()).issuePreAuthCsrf();
    }

    @Test
    void shouldFallBackToPreAuthAndClearSession_whenSessionCookieIsInvalid() {
        when(identityApi.validateSession(eq("stale-session"), any()))
                .thenThrow(new AdminSessionException());
        when(identityApi.issuePreAuthCsrf()).thenReturn(new CsrfToken("pre-auth-token", 900));

        ResponseEntity<Void> response =
                controller.csrf("stale-session", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cookies(response))
                .anySatisfy(
                        cookie ->
                                assertThat(cookie)
                                        .startsWith("ADMIN_SESSION=;")
                                        .contains("Max-Age=0"));
        assertThat(cookies(response))
                .anySatisfy(
                        cookie -> assertThat(cookie).startsWith("XSRF-TOKEN=pre-auth-token;"));
    }

    private static List<String> cookies(ResponseEntity<Void> response) {
        List<String> values = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(values).isNotNull();
        return values;
    }
}
