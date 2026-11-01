package com.hotel.adminidentity.internal.web;

import com.hotel.adminidentity.api.AdminCookies;
import com.hotel.adminidentity.api.AdminIdentityApi.CsrfToken;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AdminCookieFactory {

    private final boolean secure;

    public AdminCookieFactory(@Value("${admin.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    public ResponseCookie session(String rawToken) {
        return ResponseCookie.from(AdminCookies.SESSION_COOKIE, rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(8))
                .build();
    }

    public ResponseCookie csrf(CsrfToken token) {
        return ResponseCookie.from(AdminCookies.CSRF_COOKIE, token.value())
                .httpOnly(false)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(token.maxAgeSeconds()))
                .build();
    }

    public ResponseCookie clearSession() {
        return ResponseCookie.from(AdminCookies.SESSION_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public ResponseCookie clearCsrf() {
        return ResponseCookie.from(AdminCookies.CSRF_COOKIE, "")
                .httpOnly(false)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
