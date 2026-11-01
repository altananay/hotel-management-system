package com.hotel.config;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fails fast when the web application starts outside the local/CI profiles with insecure defaults.
 * The known CSRF secret and a non-Secure cookie are only acceptable for local development and the
 * CI stack; production must supply its own secret and enable Secure cookies.
 */
@Component
@Profile("!local & !ci")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AdminProductionSecurityValidator {

    static final String LOCAL_DEFAULT_CSRF_SECRET = "local-admin-csrf-secret-32-bytes-min!!";
    private static final int MIN_CSRF_SECRET_BYTES = 32;

    public AdminProductionSecurityValidator(
            @Value("${admin.cookie.secure:false}") boolean cookieSecure,
            @Value("${admin.csrf.secret:}") String csrfSecret) {
        if (!cookieSecure) {
            throw new IllegalStateException(
                    "ADMIN_COOKIE_SECURE must be true outside the local/CI profiles.");
        }
        if (csrfSecret == null
                || csrfSecret.getBytes(StandardCharsets.UTF_8).length < MIN_CSRF_SECRET_BYTES
                || LOCAL_DEFAULT_CSRF_SECRET.equals(csrfSecret)) {
            throw new IllegalStateException(
                    "ADMIN_CSRF_SECRET must be set to a non-default value of at least 32 bytes "
                            + "outside the local/CI profiles.");
        }
    }
}
