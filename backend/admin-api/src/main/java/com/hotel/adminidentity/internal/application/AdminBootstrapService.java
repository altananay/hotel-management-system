package com.hotel.adminidentity.internal.application;

import com.fasterxml.uuid.Generators;
import com.hotel.adminaudit.api.AdminAuditApi;
import com.hotel.adminaudit.api.AuditReasonCode;
import com.hotel.adminaudit.api.AuditResult;
import com.hotel.adminidentity.api.AdminBootstrapApi;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminBootstrapCommand;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminBootstrapOutcome;
import com.hotel.adminidentity.internal.domain.AdminRole;
import com.hotel.adminidentity.internal.domain.EmailPolicy;
import com.hotel.adminidentity.internal.domain.InvalidIdentityException;
import com.hotel.adminidentity.internal.domain.InvalidPasswordException;
import com.hotel.adminidentity.internal.domain.PasswordPolicy;
import com.hotel.adminidentity.internal.domain.UsernamePolicy;
import com.hotel.adminidentity.internal.persistence.AdminUserEntity;
import com.hotel.adminidentity.internal.persistence.AdminUserRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapService implements AdminBootstrapApi {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final AdminUserRepository userRepository;
    private final AdminUserWriter userWriter;
    private final AdminAuditApi auditApi;
    private final Clock clock;

    public AdminBootstrapService(
            AdminUserRepository userRepository,
            AdminUserWriter userWriter,
            AdminAuditApi auditApi,
            Clock clock) {
        this.userRepository = userRepository;
        this.userWriter = userWriter;
        this.auditApi = auditApi;
        this.clock = clock;
    }

    @Override
    public AdminBootstrapOutcome bootstrap(AdminBootstrapCommand command) {
        if (userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)) {
            auditSkip();
            return AdminBootstrapOutcome.SKIPPED;
        }
        if (userRepository.count() > 0) {
            return AdminBootstrapOutcome.GUARD_FAILED;
        }
        String username;
        String email;
        String password;
        try {
            username = UsernamePolicy.normalizeAndValidate(command.username());
            email = EmailPolicy.normalizeAndValidate(command.email());
            password = PasswordPolicy.validate(command.password(), username, email);
        } catch (InvalidPasswordException | InvalidIdentityException ex) {
            return AdminBootstrapOutcome.GUARD_FAILED;
        }

        AdminUserEntity created;
        try {
            created = userWriter.createSuperAdmin(username, email, password);
        } catch (InvalidIdentityException ex) {
            return AdminBootstrapOutcome.GUARD_FAILED;
        } catch (DataIntegrityViolationException ex) {
            if (userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)) {
                auditSkip();
                return AdminBootstrapOutcome.SKIPPED;
            }
            return AdminBootstrapOutcome.GUARD_FAILED;
        }

        try {
            auditApi.record(
                    new AdminAuditApi.AuditEvent(
                            clock.instant(),
                            AuditReasonCode.BOOTSTRAP_CREATED,
                            AuditResult.SUCCESS,
                            created.getId(),
                            created.getUsername(),
                            "0.0.0.0",
                            "admin-bootstrap-cli",
                            "bootstrap"));
        } catch (RuntimeException ex) {
            userWriter.delete(created.getId());
            return AdminBootstrapOutcome.AUDIT_FAILED;
        }
        return AdminBootstrapOutcome.CREATED;
    }

    private void auditSkip() {
        try {
            auditApi.record(
                    new AdminAuditApi.AuditEvent(
                            clock.instant(),
                            AuditReasonCode.BOOTSTRAP_SKIPPED,
                            AuditResult.SUCCESS,
                            null,
                            null,
                            "0.0.0.0",
                            "admin-bootstrap-cli",
                            "bootstrap"));
        } catch (RuntimeException ex) {
            log.warn("Bootstrap skip audit could not be written; continuing", ex);
        }
    }

}
