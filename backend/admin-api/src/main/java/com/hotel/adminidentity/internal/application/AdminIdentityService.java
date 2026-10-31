package com.hotel.adminidentity.internal.application;

import com.fasterxml.uuid.Generators;
import com.hotel.adminaudit.api.AdminAuditApi;
import com.hotel.adminaudit.api.AuditReasonCode;
import com.hotel.adminaudit.api.AuditResult;
import com.hotel.adminidentity.api.AdminAuditException;
import com.hotel.adminidentity.api.AdminAuthenticationException;
import com.hotel.adminidentity.api.AdminIdentityApi;
import com.hotel.adminidentity.api.AdminIdentityApi.CsrfToken;
import com.hotel.adminidentity.api.AdminSessionException;
import com.hotel.adminidentity.internal.domain.AdminUser;
import com.hotel.adminidentity.internal.domain.Argon2PasswordHasher;
import com.hotel.adminidentity.internal.persistence.AdminLoginAttemptEntity;
import com.hotel.adminidentity.internal.persistence.AdminLoginAttemptRepository;
import com.hotel.adminidentity.internal.persistence.AdminUserEntity;
import com.hotel.adminidentity.internal.persistence.AdminUserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AdminIdentityService implements AdminIdentityApi {

    private static final int MAX_FAILURES = 5;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final AdminUserRepository userRepository;
    private final AdminLoginAttemptRepository attemptRepository;
    private final Argon2PasswordHasher passwordHasher;
    private final AdminSessionService sessionService;
    private final AdminJwtService jwtService;
    private final AdminCsrfService csrfService;
    private final AdminAuditApi auditApi;
    private final AdminBootstrapService bootstrapService;
    private final Clock clock;

    public AdminIdentityService(
            AdminUserRepository userRepository,
            AdminLoginAttemptRepository attemptRepository,
            Argon2PasswordHasher passwordHasher,
            AdminSessionService sessionService,
            AdminJwtService jwtService,
            AdminCsrfService csrfService,
            AdminAuditApi auditApi,
            AdminBootstrapService bootstrapService,
            Clock clock) {
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.passwordHasher = passwordHasher;
        this.sessionService = sessionService;
        this.jwtService = jwtService;
        this.csrfService = csrfService;
        this.auditApi = auditApi;
        this.bootstrapService = bootstrapService;
        this.clock = clock;
    }

    @Override
    public AdminLoginResult login(AdminLoginCommand command) {
        String normalizedIdentifier = normalize(command.identifier());
        String ip = command.context().ipAddress();
        AdminUser user = findUser(normalizedIdentifier);
        String lockoutKey = user == null ? normalizedIdentifier : user.id().toString();

        if (isLocked(lockoutKey, ip)) {
            audit(
                    AuditReasonCode.LOGIN_LOCKED,
                    user == null ? null : user.id(),
                    normalizedIdentifier,
                    command.context());
            throw new AdminAuthenticationException("LOGIN_LOCKED");
        }

        String encodedHash = user == null ? passwordHasher.dummyHash() : user.passwordHash();
        boolean passwordMatches = passwordHasher.matches(command.password(), encodedHash);
        boolean success = user != null && user.active() && passwordMatches;
        if (!success) {
            recordFailure(lockoutKey, user, ip);
            AuditReasonCode reason =
                    user == null || user.active()
                            ? AuditReasonCode.LOGIN_INVALID_CREDENTIALS
                            : AuditReasonCode.LOGIN_INACTIVE;
            audit(reason, user == null ? null : user.id(), normalizedIdentifier, command.context());
            throw new AdminAuthenticationException(reason.name());
        }

        attemptRepository.deleteByIdentifierNormAndIpAddress(lockoutKey, ip);
        if (command.currentSessionToken() != null && !command.currentSessionToken().isBlank()) {
            sessionService.revoke(command.currentSessionToken(), command.context());
        }
        AdminLoginResult result = sessionService.createSession(user);
        try {
            audit(
                    AuditReasonCode.LOGIN_SUCCESS,
                    user.id(),
                    normalizedIdentifier,
                    command.context());
        } catch (RuntimeException ex) {
            sessionService.revoke(result.rawSessionToken(), command.context());
            throw new AdminAuditException(ex);
        }
        return result;
    }

    @Override
    public AdminSessionView validateSession(
            String rawSessionToken, AdminClientContext context) {
        try {
            return sessionService.validate(rawSessionToken, context);
        } catch (AdminSessionException ex) {
            throw ex;
        }
    }

    @Override
    public void logout(String rawSessionToken, AdminClientContext context) {
        sessionService.logout(rawSessionToken, context);
    }

    @Override
    public String issueJwt(AdminSessionView session) {
        return jwtService.issue(session);
    }

    @Override
    public CsrfToken issuePreAuthCsrf() {
        return csrfService.issuePreAuth();
    }

    @Override
    public CsrfToken issueSessionCsrf(AdminSessionView session) {
        return csrfService.issueForSession(session);
    }

    @Override
    public void validateCsrf(
            String token,
            String rawSessionToken,
            String origin,
            String method,
            boolean loginRequest) {
        csrfService.validate(token, rawSessionToken, origin, method, loginRequest);
    }

    @Override
    public AdminBootstrapOutcome bootstrap(AdminBootstrapCommand command) {
        return bootstrapService.bootstrap(command);
    }

    private AdminUser findUser(String normalizedIdentifier) {
        return userRepository
                .findByUsernameIgnoreCase(normalizedIdentifier)
                .or(() -> userRepository.findByEmailIgnoreCase(normalizedIdentifier))
                .map(AdminUserEntity::toDomain)
                .orElse(null);
    }

    private boolean isLocked(String key, String ip) {
        return !attemptRepository.findActiveLocks(key, ip, clock.instant()).isEmpty();
    }

    private void recordFailure(String key, AdminUser user, String ip) {
        Instant now = clock.instant();
        long recentFailures =
                attemptRepository.countRecentFailures(key, ip, now.minus(FAILURE_WINDOW));
        Instant lockedUntil =
                recentFailures + 1 >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null;
        attemptRepository.save(
                new AdminLoginAttemptEntity(
                        Generators.timeBasedEpochGenerator().generate(),
                        key,
                        user == null ? null : user.id(),
                        ip,
                        now,
                        lockedUntil));
    }

    private void audit(
            AuditReasonCode reason,
            UUID actorId,
            String usernameAttempted,
            AdminClientContext context) {
        auditApi.record(
                new AdminAuditApi.AuditEvent(
                        clock.instant(),
                        reason,
                        reason.defaultResult(),
                        actorId,
                        usernameAttempted,
                        context.ipAddress(),
                        context.userAgent(),
                        context.requestId()));
    }

    private String normalize(String identifier) {
        return identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
    }
}
