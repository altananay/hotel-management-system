package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotel.adminaudit.api.AdminAuditApi;
import com.hotel.adminaudit.api.AdminAuditApi.AuditEvent;
import com.hotel.adminaudit.api.AuditReasonCode;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminBootstrapCommand;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminBootstrapOutcome;
import com.hotel.adminidentity.internal.application.AdminBootstrapService;
import com.hotel.adminidentity.internal.application.AdminUserWriter;
import com.hotel.adminidentity.internal.domain.AdminRole;
import com.hotel.adminidentity.internal.persistence.AdminUserEntity;
import com.hotel.adminidentity.internal.persistence.AdminUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

    private static final String VALID_PASSWORD = "LocalOnly-Cappadocia-2026!";

    @Mock private AdminUserRepository userRepository;
    @Mock private AdminUserWriter userWriter;
    @Mock private AdminAuditApi auditApi;

    private AdminBootstrapService service;

    @BeforeEach
    void setUp() {
        service =
                new AdminBootstrapService(
                        userRepository,
                        userWriter,
                        auditApi,
                        Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void shouldSkipWithoutReadingEnv_whenActiveSuperAdminExists() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(true);

        AdminBootstrapOutcome outcome =
                service.bootstrap(new AdminBootstrapCommand("", "", ""));

        assertThat(outcome).isEqualTo(AdminBootstrapOutcome.SKIPPED);
        verify(userWriter, never()).createSuperAdmin(anyString(), anyString(), anyString());
        assertThat(auditReason()).isEqualTo(AuditReasonCode.BOOTSTRAP_SKIPPED);
    }

    @Test
    void shouldFailFast_whenOnlyPassiveSuperAdminExists() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.count()).thenReturn(1L);

        AdminBootstrapOutcome outcome =
                service.bootstrap(
                        new AdminBootstrapCommand(
                                "superadmin", "superadmin@example.com", VALID_PASSWORD));

        assertThat(outcome).isEqualTo(AdminBootstrapOutcome.GUARD_FAILED);
        verify(userWriter, never()).createSuperAdmin(anyString(), anyString(), anyString());
    }

    @Test
    void shouldFailFast_whenOnlyManagerOrFrontDeskRowsExist() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.count()).thenReturn(2L);

        AdminBootstrapOutcome outcome =
                service.bootstrap(
                        new AdminBootstrapCommand(
                                "superadmin", "superadmin@example.com", VALID_PASSWORD));

        assertThat(outcome).isEqualTo(AdminBootstrapOutcome.GUARD_FAILED);
    }

    @Test
    void shouldFailFast_whenEnvIsMissingOrWeak() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);

        assertThat(
                        service.bootstrap(
                                new AdminBootstrapCommand(
                                        "", "superadmin@example.com", VALID_PASSWORD)))
                .isEqualTo(AdminBootstrapOutcome.GUARD_FAILED);
        assertThat(
                        service.bootstrap(
                                new AdminBootstrapCommand(
                                        "superadmin", "", VALID_PASSWORD)))
                .isEqualTo(AdminBootstrapOutcome.GUARD_FAILED);
        assertThat(
                        service.bootstrap(
                                new AdminBootstrapCommand(
                                        "superadmin", "superadmin@example.com", "short")))
                .isEqualTo(AdminBootstrapOutcome.GUARD_FAILED);
    }

    @Test
    void shouldFailFast_whenUsernameOrEmailFormatIsInvalid() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);

        assertThat(
                        service.bootstrap(
                                new AdminBootstrapCommand(
                                        "-superadmin",
                                        "superadmin@example.com",
                                        VALID_PASSWORD)))
                .isEqualTo(AdminBootstrapOutcome.GUARD_FAILED);
        assertThat(
                        service.bootstrap(
                                new AdminBootstrapCommand(
                                        "superadmin", "not-an-email", VALID_PASSWORD)))
                .isEqualTo(AdminBootstrapOutcome.GUARD_FAILED);
    }

    @Test
    void shouldCreateSuperAdminWithNormalizedIdentity_whenNoAdminRowsAndEnvIsValid() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        AdminUserEntity created = createdAdmin();
        when(userWriter.createSuperAdmin("superadmin", "superadmin@example.com", VALID_PASSWORD))
                .thenReturn(created);

        AdminBootstrapOutcome outcome =
                service.bootstrap(
                        new AdminBootstrapCommand(
                                " SuperAdmin ", " SuperAdmin@Example.COM ", VALID_PASSWORD));

        assertThat(outcome).isEqualTo(AdminBootstrapOutcome.CREATED);
        verify(userWriter)
                .createSuperAdmin(
                        "superadmin", "superadmin@example.com", VALID_PASSWORD);
        assertThat(auditReason()).isEqualTo(AuditReasonCode.BOOTSTRAP_CREATED);
    }

    @Test
    void shouldSkip_whenConcurrentInsertHitsUniqueAndActiveSuperAdminExists() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN))
                .thenReturn(false, true);
        when(userRepository.count()).thenReturn(0L);
        when(userWriter.createSuperAdmin(anyString(), anyString(), anyString()))
                .thenThrow(new DataIntegrityViolationException("duplicate username"));

        AdminBootstrapOutcome outcome =
                service.bootstrap(
                        new AdminBootstrapCommand(
                                "superadmin", "superadmin@example.com", VALID_PASSWORD));

        assertThat(outcome).isEqualTo(AdminBootstrapOutcome.SKIPPED);
        assertThat(auditReason()).isEqualTo(AuditReasonCode.BOOTSTRAP_SKIPPED);
    }

    @Test
    void shouldFailFast_whenConcurrentInsertHitsUniqueWithoutActiveSuperAdmin() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN))
                .thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        when(userWriter.createSuperAdmin(anyString(), anyString(), anyString()))
                .thenThrow(new DataIntegrityViolationException("duplicate username"));

        AdminBootstrapOutcome outcome =
                service.bootstrap(
                        new AdminBootstrapCommand(
                                "superadmin", "superadmin@example.com", VALID_PASSWORD));

        assertThat(outcome).isEqualTo(AdminBootstrapOutcome.GUARD_FAILED);
    }

    @Test
    void shouldDeleteAdminAndFail_whenCreationAuditFails() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        AdminUserEntity created = createdAdmin();
        when(userWriter.createSuperAdmin(anyString(), anyString(), anyString()))
                .thenReturn(created);
        doThrow(new RuntimeException("audit down"))
                .when(auditApi)
                .record(any(AuditEvent.class));

        AdminBootstrapOutcome outcome =
                service.bootstrap(
                        new AdminBootstrapCommand(
                                "superadmin", "superadmin@example.com", VALID_PASSWORD));

        assertThat(outcome).isEqualTo(AdminBootstrapOutcome.AUDIT_FAILED);
        verify(userWriter).delete(created.getId());
    }

    @Test
    void shouldKeepSkippedOutcome_whenSkipAuditFails() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(true);
        doThrow(new RuntimeException("audit down"))
                .when(auditApi)
                .record(any(AuditEvent.class));

        AdminBootstrapOutcome outcome =
                service.bootstrap(new AdminBootstrapCommand("", "", ""));

        assertThat(outcome).isEqualTo(AdminBootstrapOutcome.SKIPPED);
    }

    private AdminUserEntity createdAdmin() {
        return new AdminUserEntity(
                UUID.randomUUID(),
                "superadmin",
                "superadmin@example.com",
                "hash",
                AdminRole.SUPER_ADMIN,
                true,
                Instant.now(),
                Instant.now());
    }

    private AuditReasonCode auditReason() {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditApi, org.mockito.Mockito.atLeastOnce()).record(captor.capture());
        return captor.getValue().reasonCode();
    }
}
