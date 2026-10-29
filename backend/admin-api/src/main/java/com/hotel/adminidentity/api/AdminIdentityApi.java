package com.hotel.adminidentity.api;

import java.time.Instant;
import java.util.UUID;

public interface AdminIdentityApi {

    AdminLoginResult login(AdminLoginCommand command);

    AdminSessionView validateSession(String rawSessionToken, AdminClientContext context);

    void logout(String rawSessionToken, AdminClientContext context);

    AdminBootstrapOutcome bootstrap(AdminBootstrapCommand command);

    record AdminLoginCommand(
            String identifier,
            String password,
            String currentSessionToken,
            AdminClientContext context) {}

    record AdminClientContext(String ipAddress, String userAgent, String requestId) {}

    record AdminSessionView(
            UUID userId,
            UUID sessionId,
            String username,
            String email,
            String role) {}

    record AdminLoginResult(AdminSessionView session, String rawSessionToken) {}

    record AdminBootstrapCommand(String username, String email, String password) {}

    enum AdminBootstrapOutcome {
        CREATED(0),
        SKIPPED(0),
        GUARD_FAILED(2),
        AUDIT_FAILED(4);

        private final int exitCode;

        AdminBootstrapOutcome(int exitCode) {
            this.exitCode = exitCode;
        }

        public int exitCode() {
            return exitCode;
        }
    }
}
