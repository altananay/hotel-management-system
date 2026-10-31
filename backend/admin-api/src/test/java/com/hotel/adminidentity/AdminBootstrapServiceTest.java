package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotel.adminaudit.api.AdminAuditApi;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

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
        verify(auditApi).record(any(AdminAuditApi.AuditEvent.class));
    }

    @Test
    void shouldFailFast_whenOnlyNonSuperAdminRowsExist() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.count()).thenReturn(1L);

        AdminBootstrapOutcome outcome =
                service.bootstrap(
                        new AdminBootstrapCommand("superadmin", "superadmin@example.com", "LocalOnly-Cappadocia-2026!"));

        assertThat(outcome).isEqualTo(AdminBootstrapOutcome.GUARD_FAILED);
        verify(userWriter, never()).createSuperAdmin(anyString(), anyString(), anyString());
    }

    @Test
    void shouldCreateSuperAdmin_whenNoAdminRowsAndEnvIsValid() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        AdminUserEntity created =
                new AdminUserEntity(
                        UUID.randomUUID(),
                        "superadmin",
                        "superadmin@example.com",
                        "hash",
                        AdminRole.SUPER_ADMIN,
                        true,
                        Instant.now(),
                        Instant.now());
        when(userWriter.createSuperAdmin(anyString(), anyString(), anyString()))
                .thenReturn(created);

        AdminBootstrapOutcome outcome =
                service.bootstrap(
                        new AdminBootstrapCommand("superadmin", "superadmin@example.com", "LocalOnly-Cappadocia-2026!"));

        assertThat(outcome).isEqualTo(AdminBootstrapOutcome.CREATED);
        verify(auditApi).record(any(AdminAuditApi.AuditEvent.class));
    }

    @Test
    void shouldDeleteAdminAndFail_whenCreationAuditFails() {
        when(userRepository.existsByRoleAndActiveTrue(AdminRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        AdminUserEntity created =
                new AdminUserEntity(
                        UUID.randomUUID(),
                        "superadmin",
                        "superadmin@example.com",
                        "hash",
                        AdminRole.SUPER_ADMIN,
                        true,
                        Instant.now(),
                        Instant.now());
        when(userWriter.createSuperAdmin(anyString(), anyString(), anyString()))
                .thenReturn(created);
        org.mockito.Mockito.doThrow(new RuntimeException("audit down"))
                .when(auditApi)
                .record(any(AdminAuditApi.AuditEvent.class));

        AdminBootstrapOutcome outcome =
                service.bootstrap(
                        new AdminBootstrapCommand("superadmin", "superadmin@example.com", "LocalOnly-Cappadocia-2026!"));

        assertThat(outcome).isEqualTo(AdminBootstrapOutcome.AUDIT_FAILED);
        verify(userWriter).delete(created.getId());
    }
}
