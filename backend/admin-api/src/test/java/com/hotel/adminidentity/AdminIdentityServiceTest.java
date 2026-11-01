package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.hotel.adminidentity.api.AdminAuditException;
import com.hotel.adminidentity.api.AdminAuthenticationException;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminClientContext;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminLoginCommand;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminLoginResult;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminSessionView;
import com.hotel.adminidentity.internal.application.AdminBootstrapService;
import com.hotel.adminidentity.internal.application.AdminCsrfService;
import com.hotel.adminidentity.internal.application.AdminIdentityService;
import com.hotel.adminidentity.internal.application.AdminJwtService;
import com.hotel.adminidentity.internal.application.AdminSessionService;
import com.hotel.adminidentity.internal.domain.AdminRole;
import com.hotel.adminidentity.internal.domain.Argon2PasswordHasher;
import com.hotel.adminidentity.internal.persistence.AdminLoginAttemptEntity;
import com.hotel.adminidentity.internal.persistence.AdminLoginAttemptRepository;
import com.hotel.adminidentity.internal.persistence.AdminUserEntity;
import com.hotel.adminidentity.internal.persistence.AdminUserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminIdentityServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");
    private static final String IP = "127.0.0.1";
    private static final String PASSWORD = "LocalOnly-Cappadocia-2026!";

    @Mock private AdminUserRepository userRepository;
    @Mock private AdminLoginAttemptRepository attemptRepository;
    @Mock private Argon2PasswordHasher passwordHasher;
    @Mock private AdminSessionService sessionService;
    @Mock private AdminJwtService jwtService;
    @Mock private AdminCsrfService csrfService;
    @Mock private AdminAuditApi auditApi;
    @Mock private AdminBootstrapService bootstrapService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final UUID userId = UUID.randomUUID();
    private final AdminClientContext context = new AdminClientContext(IP, "JUnit", "req-1");
    private AdminIdentityService service;

    @BeforeEach
    void setUp() {
        service =
                new AdminIdentityService(
                        userRepository,
                        attemptRepository,
                        passwordHasher,
                        sessionService,
                        jwtService,
                        csrfService,
                        auditApi,
                        bootstrapService,
                        clock);
    }

    @Test
    void shouldCreateSessionClearFailuresAndAudit_whenCredentialsAreValid() {
        AdminUserEntity user = userEntity(true);
        when(userRepository.findByUsernameIgnoreCase("superadmin")).thenReturn(Optional.of(user));
        when(attemptRepository.findActiveLocks("superadmin", IP, NOW)).thenReturn(List.of());
        when(passwordHasher.matches(PASSWORD, "hash")).thenReturn(true);
        AdminSessionView view =
                new AdminSessionView(
                        userId, UUID.randomUUID(), "superadmin", "superadmin@example.com", "SUPER_ADMIN");
        when(sessionService.createSession(user.toDomain()))
                .thenReturn(new AdminLoginResult(view, "raw-new"));

        AdminLoginResult result =
                service.login(new AdminLoginCommand(" superadmin ", PASSWORD, "raw-old", context));

        assertThat(result.rawSessionToken()).isEqualTo("raw-new");
        assertThat(result.session()).isEqualTo(view);
        verify(attemptRepository).deleteByIdentifierNormAndIpAddress("superadmin", IP);
        verify(sessionService).revoke("raw-old", context);
        AuditEvent event = auditEvent(AuditReasonCode.LOGIN_SUCCESS);
        assertThat(event.actorUserId()).isEqualTo(userId);
        assertThat(event.usernameAttempted()).isEqualTo("superadmin");
        assertThat(event.ipAddress()).isEqualTo(IP);
        assertThat(event.requestId()).isEqualTo("req-1");
    }

    @Test
    void shouldRevokeNewSessionAndFail_whenSuccessAuditFails() {
        AdminUserEntity user = userEntity(true);
        when(userRepository.findByUsernameIgnoreCase("superadmin")).thenReturn(Optional.of(user));
        when(attemptRepository.findActiveLocks("superadmin", IP, NOW)).thenReturn(List.of());
        when(passwordHasher.matches(PASSWORD, "hash")).thenReturn(true);
        AdminSessionView view =
                new AdminSessionView(
                        userId, UUID.randomUUID(), "superadmin", "superadmin@example.com", "SUPER_ADMIN");
        when(sessionService.createSession(user.toDomain()))
                .thenReturn(new AdminLoginResult(view, "raw-new"));
        doThrow(new IllegalStateException("audit down"))
                .when(auditApi)
                .record(any(AuditEvent.class));

        assertThatThrownBy(
                        () ->
                                service.login(
                                        new AdminLoginCommand(
                                                "superadmin", PASSWORD, null, context)))
                .isInstanceOf(AdminAuditException.class);
        verify(sessionService).revoke("raw-new", context);
    }

    @Test
    void shouldUseDummyHashAndAuditFailure_whenUserIsUnknown() {
        when(userRepository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ghost")).thenReturn(Optional.empty());
        when(attemptRepository.findActiveLocks("ghost", IP, NOW)).thenReturn(List.of());
        when(passwordHasher.dummyHash()).thenReturn("dummy-hash");
        when(passwordHasher.matches("wrong", "dummy-hash")).thenReturn(false);
        when(attemptRepository.countRecentFailures(
                        "ghost", IP, NOW.minus(Duration.ofMinutes(15))))
                .thenReturn(0L);

        assertThatThrownBy(
                        () ->
                                service.login(
                                        new AdminLoginCommand("Ghost", "wrong", null, context)))
                .isInstanceOfSatisfying(
                        AdminAuthenticationException.class,
                        ex -> assertThat(ex.reasonCode()).isEqualTo("LOGIN_INVALID_CREDENTIALS"));

        ArgumentCaptor<AdminLoginAttemptEntity> attempt =
                ArgumentCaptor.forClass(AdminLoginAttemptEntity.class);
        verify(attemptRepository).save(attempt.capture());
        assertThat(attempt.getValue().getIdentifierNorm()).isEqualTo("ghost");
        assertThat(attempt.getValue().getUserId()).isNull();
        assertThat(attempt.getValue().getLockedUntil()).isNull();
        AuditEvent event = auditEvent(AuditReasonCode.LOGIN_INVALID_CREDENTIALS);
        assertThat(event.actorUserId()).isNull();
        assertThat(event.usernameAttempted()).isEqualTo("ghost");
    }

    @Test
    void shouldLockForFifteenMinutes_whenFifthFailureOccursWithinWindow() {
        AdminUserEntity user = userEntity(true);
        when(userRepository.findByUsernameIgnoreCase("superadmin")).thenReturn(Optional.of(user));
        when(attemptRepository.findActiveLocks("superadmin", IP, NOW)).thenReturn(List.of());
        when(passwordHasher.matches("wrong", "hash")).thenReturn(false);
        when(attemptRepository.countRecentFailures(
                        "superadmin", IP, NOW.minus(Duration.ofMinutes(15))))
                .thenReturn(4L);

        assertThatThrownBy(
                        () ->
                                service.login(
                                        new AdminLoginCommand("superadmin", "wrong", null, context)))
                .isInstanceOf(AdminAuthenticationException.class);

        ArgumentCaptor<AdminLoginAttemptEntity> attempt =
                ArgumentCaptor.forClass(AdminLoginAttemptEntity.class);
        verify(attemptRepository).save(attempt.capture());
        assertThat(attempt.getValue().getLockedUntil())
                .isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(attempt.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldRejectWithGenericErrorAndSkipPasswordCheck_whenIdentifierIsLocked() {
        AdminUserEntity user = userEntity(true);
        when(userRepository.findByUsernameIgnoreCase("superadmin")).thenReturn(Optional.of(user));
        when(attemptRepository.findActiveLocks("superadmin", IP, NOW))
                .thenReturn(
                        List.of(
                                new AdminLoginAttemptEntity(
                                        UUID.randomUUID(),
                                        "superadmin",
                                        userId,
                                        IP,
                                        NOW.minus(Duration.ofMinutes(1)),
                                        NOW.plus(Duration.ofMinutes(14)))));

        assertThatThrownBy(
                        () ->
                                service.login(
                                        new AdminLoginCommand(
                                                "superadmin", PASSWORD, null, context)))
                .isInstanceOfSatisfying(
                        AdminAuthenticationException.class,
                        ex -> assertThat(ex.reasonCode()).isEqualTo("LOGIN_LOCKED"));
        verifyNoInteractions(passwordHasher);
        verify(sessionService, never()).createSession(any());
        assertThat(auditEvent(AuditReasonCode.LOGIN_LOCKED).actorUserId()).isEqualTo(userId);
    }

    @Test
    void shouldUseSameUserCounter_whenLoggingInWithEmailAlias() {
        AdminUserEntity user = userEntity(true);
        when(userRepository.findByUsernameIgnoreCase("superadmin@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("superadmin@example.com"))
                .thenReturn(Optional.of(user));
        when(attemptRepository.findActiveLocks("superadmin", IP, NOW)).thenReturn(List.of());
        when(passwordHasher.matches("wrong", "hash")).thenReturn(false);
        when(attemptRepository.countRecentFailures(
                        "superadmin", IP, NOW.minus(Duration.ofMinutes(15))))
                .thenReturn(0L);

        assertThatThrownBy(
                        () ->
                                service.login(
                                        new AdminLoginCommand(
                                                "SuperAdmin@Example.com", "wrong", null, context)))
                .isInstanceOf(AdminAuthenticationException.class);

        ArgumentCaptor<AdminLoginAttemptEntity> attempt =
                ArgumentCaptor.forClass(AdminLoginAttemptEntity.class);
        verify(attemptRepository).save(attempt.capture());
        assertThat(attempt.getValue().getIdentifierNorm()).isEqualTo("superadmin");
        assertThat(attempt.getValue().getUserId()).isEqualTo(userId);
        assertThat(auditEvent(AuditReasonCode.LOGIN_INVALID_CREDENTIALS).usernameAttempted())
                .isEqualTo("superadmin@example.com");
    }

    @Test
    void shouldAuditInactiveUser_whenCredentialsMatchButUserIsInactive() {
        AdminUserEntity user = userEntity(false);
        when(userRepository.findByUsernameIgnoreCase("superadmin")).thenReturn(Optional.of(user));
        when(attemptRepository.findActiveLocks("superadmin", IP, NOW)).thenReturn(List.of());
        when(passwordHasher.matches(PASSWORD, "hash")).thenReturn(true);
        when(attemptRepository.countRecentFailures(
                        "superadmin", IP, NOW.minus(Duration.ofMinutes(15))))
                .thenReturn(0L);

        assertThatThrownBy(
                        () ->
                                service.login(
                                        new AdminLoginCommand(
                                                "superadmin", PASSWORD, null, context)))
                .isInstanceOfSatisfying(
                        AdminAuthenticationException.class,
                        ex -> assertThat(ex.reasonCode()).isEqualTo("LOGIN_INACTIVE"));
        verify(sessionService, never()).createSession(any());
        assertThat(auditEvent(AuditReasonCode.LOGIN_INACTIVE).actorUserId()).isEqualTo(userId);
    }

    @Test
    void shouldFailWithAuditErrorAndCreateNoSession_whenFailureAuditFails() {
        when(userRepository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ghost")).thenReturn(Optional.empty());
        when(attemptRepository.findActiveLocks("ghost", IP, NOW)).thenReturn(List.of());
        when(passwordHasher.dummyHash()).thenReturn("dummy-hash");
        when(passwordHasher.matches("wrong", "dummy-hash")).thenReturn(false);
        when(attemptRepository.countRecentFailures(
                        "ghost", IP, NOW.minus(Duration.ofMinutes(15))))
                .thenReturn(0L);
        doThrow(new IllegalStateException("audit down"))
                .when(auditApi)
                .record(any(AuditEvent.class));

        assertThatThrownBy(
                        () ->
                                service.login(
                                        new AdminLoginCommand("ghost", "wrong", null, context)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("audit down");
        verify(sessionService, never()).createSession(any());
    }

    @Test
    void shouldDelegateLogoutToSessionService() {
        service.logout("raw-session", context);

        verify(sessionService).logout("raw-session", context);
        verifyNoInteractions(auditApi);
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

    private AuditEvent auditEvent(AuditReasonCode reasonCode) {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditApi, org.mockito.Mockito.atLeastOnce()).record(captor.capture());
        return captor.getAllValues().stream()
                .filter(event -> event.reasonCode() == reasonCode)
                .findFirst()
                .orElseThrow();
    }
}
