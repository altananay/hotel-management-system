package com.hotel.adminaudit.internal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "admin_platform", name = "admin_audit")
public class AuditEntryEntity {

    @Id private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "username_attempted", length = 254)
    private String usernameAttempted;

    @Column(nullable = false, length = 10)
    private String result;

    @Column(name = "reason_code", nullable = false, length = 40)
    private String reasonCode;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    protected AuditEntryEntity() {}

    public AuditEntryEntity(
            UUID id,
            Instant occurredAt,
            String eventType,
            UUID actorUserId,
            String usernameAttempted,
            String result,
            String reasonCode,
            String ipAddress,
            String userAgent,
            String requestId) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.eventType = eventType;
        this.actorUserId = actorUserId;
        this.usernameAttempted = usernameAttempted;
        this.result = result;
        this.reasonCode = reasonCode;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.requestId = requestId;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getUsernameAttempted() {
        return usernameAttempted;
    }

    public String getResult() {
        return result;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getRequestId() {
        return requestId;
    }
}
