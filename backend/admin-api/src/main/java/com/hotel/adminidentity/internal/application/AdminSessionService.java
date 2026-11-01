package com.hotel.adminidentity.internal.application;

import com.fasterxml.uuid.Generators;
import com.hotel.adminaudit.api.AdminAuditApi;
import com.hotel.adminaudit.api.AuditReasonCode;
import com.hotel.adminaudit.api.AuditResult;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminClientContext;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminLoginResult;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminSessionView;
import com.hotel.adminidentity.api.AdminSessionException;
import com.hotel.adminidentity.internal.domain.AdminUser;
import com.hotel.adminidentity.internal.domain.SessionTokenHasher;
import com.hotel.adminidentity.internal.persistence.AdminSessionEntity;
import com.hotel.adminidentity.internal.persistence.AdminSessionRepository;
import com.hotel.adminidentity.internal.persistence.AdminUserEntity;
import com.hotel.adminidentity.internal.persistence.AdminUserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSessionService {

    private static final Logger log = LoggerFactory.getLogger(AdminSessionService.class);

    private final AdminUserRepository userRepository;
    private final AdminSessionRepository sessionRepository;
    private final SessionTokenHasher tokenHasher;
    private final AdminAuditApi auditApi;
    private final Clock clock;
    private final Duration idleTimeout;
    private final Duration absoluteTimeout;

    public AdminSessionService(
            AdminUserRepository userRepository,
            AdminSessionRepository sessionRepository,
            SessionTokenHasher tokenHasher,
            AdminAuditApi auditApi,
            Clock clock,
            @Value("${admin.session.idle-timeout:30m}") Duration idleTimeout,
            @Value("${admin.session.absolute-timeout:8h}") Duration absoluteTimeout) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.tokenHasher = tokenHasher;
        this.auditApi = auditApi;
        this.clock = clock;
        this.idleTimeout = idleTimeout;
        this.absoluteTimeout = absoluteTimeout;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AdminLoginResult createSession(AdminUser user) {
        Instant now = clock.instant();
        String rawToken = tokenHasher.newToken();
        AdminSessionEntity entity =
                new AdminSessionEntity(
                        Generators.timeBasedEpochGenerator().generate(),
                        user.id(),
                        tokenHasher.hash(rawToken),
                        now,
                        now,
                        now.plus(absoluteTimeout));
        sessionRepository.save(entity);
        return new AdminLoginResult(toView(entity, user), rawToken);
    }

    @Transactional
    public AdminSessionView validate(String rawToken, AdminClientContext context) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AdminSessionException();
        }
        AdminSessionEntity session =
                sessionRepository
                        .findByTokenHash(tokenHasher.hash(rawToken))
                        .orElseThrow(AdminSessionException::new);
        Instant now = clock.instant();
        if (!now.isBefore(session.getAbsoluteExpiresAt())
                || !now.isBefore(session.getLastSeenAt().plus(idleTimeout))) {
            sessionRepository.delete(session);
            throw new AdminSessionException();
        }
        AdminUser user =
                userRepository
                        .findById(session.getUserId())
                        .filter(AdminUserEntity::isActive)
                        .map(AdminUserEntity::toDomain)
                        .orElseThrow(AdminSessionException::new);
        session.touch(now);
        return toView(session, user);
    }

    @Transactional
    public void logout(String rawToken, AdminClientContext context) {
        AdminSessionEntity session =
                sessionRepository
                        .findByTokenHash(tokenHasher.hash(rawToken))
                        .orElse(null);
        if (session == null) {
            return;
        }
        UUID userId = session.getUserId();
        sessionRepository.delete(session);
        try {
            auditApi.record(
                    new AdminAuditApi.AuditEvent(
                            clock.instant(),
                            AuditReasonCode.LOGOUT,
                            AuditResult.SUCCESS,
                            userId,
                            null,
                            context.ipAddress(),
                            context.userAgent(),
                            context.requestId()));
        } catch (RuntimeException ex) {
            log.warn("Logout audit write failed for user {}; logout continues", userId, ex);
        }
    }

    @Transactional
    public void revoke(String rawToken, AdminClientContext context) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        AdminSessionEntity session =
                sessionRepository
                        .findByTokenHash(tokenHasher.hash(rawToken))
                        .orElse(null);
        if (session == null) {
            return;
        }
        UUID userId = session.getUserId();
        sessionRepository.delete(session);
        try {
            auditApi.record(
                    new AdminAuditApi.AuditEvent(
                            clock.instant(),
                            AuditReasonCode.SESSION_REVOKED,
                            AuditResult.SUCCESS,
                            userId,
                            null,
                            context.ipAddress(),
                            context.userAgent(),
                            context.requestId()));
        } catch (RuntimeException ex) {
            log.warn("Session-revoked audit write failed for user {}; revocation continues", userId, ex);
        }
    }

    private AdminSessionView toView(AdminSessionEntity session, AdminUser user) {
        return new AdminSessionView(
                user.id(), session.getId(), user.username(), user.email(), user.role().name());
    }
}
