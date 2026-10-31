package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hotel.adminaudit.api.AdminAuditApi;
import com.hotel.adminaudit.api.AdminAuditApi.AuditEvent;
import com.hotel.adminaudit.api.AuditReasonCode;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminClientContext;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminSessionView;
import com.hotel.adminidentity.api.AdminSessionException;
import com.hotel.adminidentity.internal.application.AdminSessionService;
import com.hotel.adminidentity.internal.domain.AdminRole;
import com.hotel.adminidentity.internal.domain.SessionTokenHasher;
import com.hotel.adminidentity.internal.persistence.AdminSessionEntity;
import com.hotel.adminidentity.internal.persistence.AdminSessionRepository;
import com.hotel.adminidentity.internal.persistence.AdminUserEntity;
import com.hotel.adminidentity.internal.persistence.AdminUserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");
    private static final String IP = "127.0.0.1";
    private static final String RAW_TOKEN = "raw-session-token";

    @Mock private AdminUserRepository userRepository;
    @Mock private AdminSessionRepository sessionRepository;
    @Mock private AdminAuditApi auditApi;

    private final SessionTokenHasher tokenHasher = new SessionTokenHasher();
    private final AdminClientContext context = new AdminClientContext(IP, "JUnit", "req-1");
    private final UUID userId = UUID.randomUUID();
    private AdminSessionService service;

    @BeforeEach
    void setUp() {
        service =
                new AdminSessionService(
                        userRepository,
                        sessionRepository,
                        tokenHasher,
                        auditApi,
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        Duration.ofMinutes(30),
                        Duration.ofHours(8));
    }

    @Test
    void shouldTouchLastSeenAndReturnView_whenSessionIsValid() {
        AdminSessionEntity session =
                session(NOW.minus(Duration.ofMinutes(5)), NOW.plus(Duration.ofHours(7)));
        AdminUserEntity user = userEntity(true);
        when(sessionRepository.findByTokenHash(tokenHasher.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(session));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AdminSessionView view = service.validate(RAW_TOKEN, context);

        assertThat(view.userId()).isEqualTo(userId);
        assertThat(view.sessionId()).isEqualTo(session.getId());
        assertThat(view.username()).isEqualTo("superadmin");
        assertThat(view.email()).isEqualTo("superadmin@example.com");
        assertThat(view.role()).isEqualTo("SUPER_ADMIN");
        assertThat(session.getLastSeenAt()).isEqualTo(NOW);
        verify(sessionRepository, never()).delete(any());
    }

    @Test
    void shouldDeleteAndReject_whenIdleTimeoutIsExceeded() {
        AdminSessionEntity session =
                session(NOW.minus(Duration.ofMinutes(31)), NOW.plus(Duration.ofHours(7)));
        when(sessionRepository.findByTokenHash(tokenHasher.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.validate(RAW_TOKEN, context))
                .isInstanceOf(AdminSessionException.class);
        verify(sessionRepository).delete(session);
        verifyNoInteractions(auditApi);
    }

    @Test
    void shouldDeleteAndReject_whenAbsoluteExpiryIsReached() {
        AdminSessionEntity session = session(NOW.minus(Duration.ofMinutes(1)), NOW);
        when(sessionRepository.findByTokenHash(tokenHasher.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.validate(RAW_TOKEN, context))
                .isInstanceOf(AdminSessionException.class);
        verify(sessionRepository).delete(session);
    }

    @Test
    void shouldRejectWithoutRepositoryLookup_whenTokenIsBlank() {
        assertThatThrownBy(() -> service.validate(" ", context))
                .isInstanceOf(AdminSessionException.class);
        verifyNoInteractions(sessionRepository, userRepository, auditApi);
    }

    @Test
    void shouldReject_whenUserIsInactive() {
        AdminSessionEntity session =
                session(NOW.minus(Duration.ofMinutes(5)), NOW.plus(Duration.ofHours(7)));
        when(sessionRepository.findByTokenHash(tokenHasher.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(session));
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity(false)));

        assertThatThrownBy(() -> service.validate(RAW_TOKEN, context))
                .isInstanceOf(AdminSessionException.class);
    }

    @Test
    void shouldDeleteOnlyCurrentSessionAndAuditLogout_whenLoggingOut() {
        AdminSessionEntity session =
                session(NOW.minus(Duration.ofMinutes(5)), NOW.plus(Duration.ofHours(7)));
        when(sessionRepository.findByTokenHash(tokenHasher.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(session));

        service.logout(RAW_TOKEN, context);

        verify(sessionRepository).delete(session);
        AuditEvent event = auditEvent();
        assertThat(event.reasonCode()).isEqualTo(AuditReasonCode.LOGOUT);
        assertThat(event.actorUserId()).isEqualTo(userId);
        assertThat(event.ipAddress()).isEqualTo(IP);
        assertThat(event.userAgent()).isEqualTo("JUnit");
        assertThat(event.requestId()).isEqualTo("req-1");
    }

    @Test
    void shouldContinueLogout_whenAuditWriteFails() {
        AdminSessionEntity session =
                session(NOW.minus(Duration.ofMinutes(5)), NOW.plus(Duration.ofHours(7)));
        when(sessionRepository.findByTokenHash(tokenHasher.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(session));
        doThrow(new IllegalStateException("audit down"))
                .when(auditApi)
                .record(any(AuditEvent.class));

        assertThatCode(() -> service.logout(RAW_TOKEN, context)).doesNotThrowAnyException();
        verify(sessionRepository).delete(session);
    }

    @Test
    void shouldIgnoreUnknownSession_whenLoggingOut() {
        when(sessionRepository.findByTokenHash(tokenHasher.hash(RAW_TOKEN)))
                .thenReturn(Optional.empty());

        service.logout(RAW_TOKEN, context);

        verify(sessionRepository, never()).delete(any());
        verifyNoInteractions(auditApi);
    }

    @Test
    void shouldDeleteRotatedSessionAndAuditRevocation_whenRevoking() {
        AdminSessionEntity session =
                session(NOW.minus(Duration.ofMinutes(5)), NOW.plus(Duration.ofHours(7)));
        when(sessionRepository.findByTokenHash(tokenHasher.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(session));

        service.revoke(RAW_TOKEN, context);

        verify(sessionRepository).delete(session);
        assertThat(auditEvent().reasonCode()).isEqualTo(AuditReasonCode.SESSION_REVOKED);
    }

    private AdminSessionEntity session(Instant lastSeenAt, Instant absoluteExpiresAt) {
        return new AdminSessionEntity(
                UUID.randomUUID(),
                userId,
                tokenHasher.hash(RAW_TOKEN),
                NOW.minus(Duration.ofHours(1)),
                lastSeenAt,
                absoluteExpiresAt);
    }

    private AdminUserEntity userEntity(boolean active) {
        return new AdminUserEntity(
                userId,
                "superadmin",
                "superadmin@example.com",
                "hash",
                AdminRole.SUPER_ADMIN,
                active,
                NOW.minus(Duration.ofDays(1)),
                NOW.minus(Duration.ofDays(1)));
    }

    private AuditEvent auditEvent() {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditApi).record(captor.capture());
        return captor.getValue();
    }
}
