package com.hotel.adminidentity.internal.web;

import com.hotel.adminidentity.api.AdminCookies;
import com.hotel.adminidentity.api.AdminIdentityApi;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminClientContext;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminLoginCommand;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminLoginResult;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminSessionView;
import com.hotel.adminidentity.api.AdminIdentityApi.CsrfToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAuthController {

    private final AdminIdentityApi identityApi;
    private final AdminCookieFactory cookieFactory;

    public AdminAuthController(
            AdminIdentityApi identityApi, AdminCookieFactory cookieFactory) {
        this.identityApi = identityApi;
        this.cookieFactory = cookieFactory;
    }

    @GetMapping("/admin-api/auth/csrf")
    public ResponseEntity<Void> csrf() {
        CsrfToken token = identityApi.issuePreAuthCsrf();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookieFactory.csrf(token).toString());
        return ResponseEntity.ok().headers(headers).build();
    }

    @PostMapping("/admin-api/auth/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequest request,
            @CookieValue(name = AdminCookies.SESSION_COOKIE, required = false)
                    String currentSessionToken,
            HttpServletRequest servletRequest) {
        AdminLoginResult result =
                identityApi.login(
                        new AdminLoginCommand(
                                request.identifier(),
                                request.password(),
                                currentSessionToken,
                                context(servletRequest)));
        CsrfToken csrfToken = identityApi.issueSessionCsrf(result.session());
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookieFactory.session(result.rawSessionToken()).toString());
        headers.add(HttpHeaders.SET_COOKIE, cookieFactory.csrf(csrfToken).toString());
        return ResponseEntity.noContent().headers(headers).build();
    }

    @PostMapping("/admin-api/auth/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AdminCookies.SESSION_COOKIE, required = false)
                    String currentSessionToken,
            HttpServletRequest servletRequest) {
        identityApi.logout(currentSessionToken, context(servletRequest));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookieFactory.clearSession().toString());
        headers.add(HttpHeaders.SET_COOKIE, cookieFactory.clearCsrf().toString());
        return ResponseEntity.noContent().headers(headers).build();
    }

    @GetMapping("/admin-api/auth/me")
    public MeResponse me(@RequestAttribute(name = "adminSession") AdminSessionView session) {
        return MeResponse.from(session);
    }

    static AdminClientContext context(HttpServletRequest request) {
        return new AdminClientContext(
                attribute(request, "adminClientIp", request.getRemoteAddr()),
                attribute(request, "adminUserAgent", request.getHeader("User-Agent")),
                attribute(request, "adminRequestId", "unknown"));
    }

    private static String attribute(HttpServletRequest request, String name, String fallback) {
        Object value = request.getAttribute(name);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }
}
