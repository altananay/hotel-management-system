package com.hotel.adminaudit.api;

public enum AuditReasonCode {
    LOGIN_SUCCESS(AuditEventType.LOGIN, AuditResult.SUCCESS),
    LOGIN_INVALID_CREDENTIALS(AuditEventType.LOGIN, AuditResult.FAILURE),
    LOGIN_LOCKED(AuditEventType.LOGIN, AuditResult.FAILURE),
    LOGIN_INACTIVE(AuditEventType.LOGIN, AuditResult.FAILURE),
    LOGOUT(AuditEventType.LOGOUT, AuditResult.SUCCESS),
    SESSION_REVOKED(AuditEventType.SESSION_REVOKED, AuditResult.SUCCESS),
    BOOTSTRAP_CREATED(AuditEventType.BOOTSTRAP, AuditResult.SUCCESS),
    BOOTSTRAP_SKIPPED(AuditEventType.BOOTSTRAP, AuditResult.SUCCESS);

    private final AuditEventType eventType;
    private final AuditResult defaultResult;

    AuditReasonCode(AuditEventType eventType, AuditResult defaultResult) {
        this.eventType = eventType;
        this.defaultResult = defaultResult;
    }

    public AuditEventType eventType() {
        return eventType;
    }

    public AuditResult defaultResult() {
        return defaultResult;
    }
}
