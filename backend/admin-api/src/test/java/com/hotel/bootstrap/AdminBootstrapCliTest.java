package com.hotel.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.hotel.adminidentity.api.AdminIdentityApi;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminBootstrapCommand;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminBootstrapOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapCliTest {

    @Mock private AdminIdentityApi identityApi;

    private AdminBootstrapCli cli;

    @BeforeEach
    void setUp() {
        cli = new AdminBootstrapCli(identityApi, "superadmin", "superadmin@example.com", "pw");
    }

    @Test
    void shouldReturnDeclaredExitCode_whenBootstrapOutcomeIsKnown() {
        when(identityApi.bootstrap(any(AdminBootstrapCommand.class)))
                .thenReturn(
                        AdminBootstrapOutcome.CREATED,
                        AdminBootstrapOutcome.SKIPPED,
                        AdminBootstrapOutcome.GUARD_FAILED,
                        AdminBootstrapOutcome.AUDIT_FAILED);

        assertThat(cli.run()).isZero();
        assertThat(cli.run()).isZero();
        assertThat(cli.run()).isEqualTo(2);
        assertThat(cli.run()).isEqualTo(4);
    }

    @Test
    void shouldReturnThree_whenBootstrapThrowsDatabaseError() {
        when(identityApi.bootstrap(any(AdminBootstrapCommand.class)))
                .thenThrow(new IllegalStateException("database down"));

        assertThat(cli.run()).isEqualTo(3);
    }
}
