package com.hotel.adminaudit.api;

import java.time.Instant;
import java.util.UUID;

public interface AdminAuditApi {

    void record(AuditEvent event);

    record AuditEvent(
            Instant occurredAt,
            AuditReasonCode reasonCode,
            AuditResult result,
            UUID actorUserId,
            String usernameAttempted,
            String ipAddress,
            String userAgent,
            String requestId) {}
}
