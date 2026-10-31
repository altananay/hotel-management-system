package com.hotel.adminidentity.internal.application;

import com.hotel.adminidentity.api.AdminCsrfException;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminSessionView;
import com.hotel.adminidentity.api.AdminIdentityApi.CsrfToken;
import com.hotel.adminidentity.internal.domain.SessionTokenHasher;
import com.hotel.adminidentity.internal.persistence.AdminSessionRepository;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminCsrfService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long SESSION_CSRF_MAX_AGE_SECONDS = 28_800;

    private final String secret;
    private final String[] allowedOrigins;
    private final Clock clock;
    private final Duration preAuthTtl;
    private final AdminSessionRepository sessionRepository;
    private final SessionTokenHasher tokenHasher;
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder decoder = Base64.getUrlDecoder();

    public AdminCsrfService(
            @Value("${admin.csrf.secret:}") String secret,
            @Value("${admin.allowed-origins:http://localhost:3001}") String allowedOrigins,
            Clock clock,
            @Value("${admin.csrf.pre-auth-ttl:15m}") Duration preAuthTtl,
            AdminSessionRepository sessionRepository,
            SessionTokenHasher tokenHasher) {
        this.secret = secret;
        this.allowedOrigins =
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toArray(String[]::new);
        this.clock = clock;
        this.preAuthTtl = preAuthTtl;
        this.sessionRepository = sessionRepository;
        this.tokenHasher = tokenHasher;
    }

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ADMIN_CSRF_SECRET must be at least 32 bytes.");
        }
    }

    public CsrfToken issuePreAuth() {
        Instant now = clock.instant();
        long expiresAt = now.plus(preAuthTtl).getEpochSecond();
        String payload = now.getEpochSecond() + "." + expiresAt + "." + nonce();
        return new CsrfToken(sign(payload), preAuthTtl.toSeconds());
    }

    public CsrfToken issueForSession(AdminSessionView session) {
        long expiresAt = clock.instant().plusSeconds(SESSION_CSRF_MAX_AGE_SECONDS).getEpochSecond();
        String payload =
                clock.instant().getEpochSecond()
                        + "."
                        + expiresAt
                        + "."
                        + nonce()
                        + "."
                        + session.sessionId();
        return new CsrfToken(sign(payload), SESSION_CSRF_MAX_AGE_SECONDS);
    }

    public void validate(
            String token,
            String rawSessionToken,
            String origin,
            String method,
            boolean loginRequest) {
        if (isSafeMethod(method)) {
            return;
        }
        if (!isAllowedOrigin(origin)) {
            throw new AdminCsrfException();
        }
        if (token == null || token.isBlank()) {
            throw new AdminCsrfException();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new AdminCsrfException();
        }
        String payloadValue = decode(parts[0]);
        byte[] expected = hmac(payloadValue).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = parts[1].getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new AdminCsrfException();
        }
        String[] payload = payloadValue.split("\\.");
        if (payload.length != 3 && payload.length != 4) {
            throw new AdminCsrfException();
        }
        long expiresAt;
        try {
            expiresAt = Long.parseLong(payload[1]);
        } catch (NumberFormatException ex) {
            throw new AdminCsrfException();
        }
        if (clock.instant().getEpochSecond() >= expiresAt) {
            throw new AdminCsrfException();
        }
        if (payload.length == 4) {
            String sessionId = payload[3];
            String currentSessionId = resolveSessionId(rawSessionToken);
            if (currentSessionId == null || !sessionId.equals(currentSessionId)) {
                throw new AdminCsrfException();
            }
            return;
        }
        if (!loginRequest) {
            throw new AdminCsrfException();
        }
    }

    private String resolveSessionId(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            return null;
        }
        return sessionRepository
                .findByTokenHash(tokenHasher.hash(rawSessionToken))
                .map(session -> session.getId().toString())
                .orElse(null);
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return false;
        }
        String normalized = origin.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(allowedOrigins)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::equals);
    }

    private boolean isSafeMethod(String method) {
        return method == null
                || method.equalsIgnoreCase("GET")
                || method.equalsIgnoreCase("HEAD")
                || method.equalsIgnoreCase("OPTIONS");
    }

    private String sign(String payload) {
        return encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "."
                + hmac(payload);
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return encoder.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign CSRF token.", ex);
        }
    }

    private String decode(String payload) {
        return new String(decoder.decode(payload), StandardCharsets.UTF_8);
    }

    private String nonce() {
        return java.util.UUID.randomUUID().toString();
    }
}
