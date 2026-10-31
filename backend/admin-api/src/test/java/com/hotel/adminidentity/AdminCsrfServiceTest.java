package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.adminidentity.api.AdminCsrfException;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminSessionView;
import com.hotel.adminidentity.internal.application.AdminCsrfService;
import com.hotel.adminidentity.internal.domain.SessionTokenHasher;
import com.hotel.adminidentity.internal.persistence.AdminSessionEntity;
import com.hotel.adminidentity.internal.persistence.AdminSessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminCsrfServiceTest {

    @Mock private AdminSessionRepository sessionRepository;

    private final SessionTokenHasher tokenHasher = new SessionTokenHasher();
    private AdminCsrfService service;

    @BeforeEach
    void setUp() {
        service =
                new AdminCsrfService(
                        "local-csrf-secret-with-at-least-32-bytes!",
                        "http://localhost:3001",
                        Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC),
                        Duration.ofMinutes(15),
                        sessionRepository,
                        tokenHasher);
    }

    @Test
    void shouldAcceptPreAuthToken_whenOriginAndLoginAreValid() {
        String token = service.issuePreAuth().value();

        assertThatCode(
                        () ->
                                service.validate(
                                        token,
                                        null,
                                        "http://localhost:3001",
                                        "POST",
                                        true))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingOriginOrPreAuthTokenOnLogout_whenStateChanging() {
        String token = service.issuePreAuth().value();

        assertThatThrownBy(() -> service.validate(token, null, null, "POST", true))
                .isInstanceOf(AdminCsrfException.class);
        assertThatThrownBy(
                        () ->
                                service.validate(
                                        token,
                                        null,
                                        "http://localhost:3001",
                                        "POST",
                                        false))
                .isInstanceOf(AdminCsrfException.class);
    }

    @Test
    void shouldSkipCsrf_whenRequestIsReadOnly() {
        assertThatCode(() -> service.validate(null, null, null, "GET", false))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptSessionBoundToken_whenSessionMatches() {
        String rawSession = tokenHasher.newToken();
        String hash = tokenHasher.hash(rawSession);
        UUID sessionId = UUID.randomUUID();
        AdminSessionEntity entity =
                new AdminSessionEntity(
                        sessionId,
                        UUID.randomUUID(),
                        hash,
                        Instant.parse("2026-10-29T09:00:00Z"),
                        Instant.parse("2026-10-29T09:00:00Z"),
                        Instant.parse("2026-10-29T17:00:00Z"));
        org.mockito.Mockito.when(sessionRepository.findByTokenHash(hash))
                .thenReturn(Optional.of(entity));
        String token =
                service.issueForSession(
                                new AdminSessionView(
                                        entity.getUserId(),
                                        sessionId,
                                        "superadmin",
                                        "superadmin@example.com",
                                        "SUPER_ADMIN"))
                        .value();

        assertThatCode(
                        () ->
                                service.validate(
                                        token,
                                        rawSession,
                                        "http://localhost:3001",
                                        "POST",
                                        false))
                .doesNotThrowAnyException();
    }
}
