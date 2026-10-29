CREATE TABLE admin_platform.admin_audit (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    actor_user_id UUID NULL,
    username_attempted VARCHAR(254) NULL,
    result VARCHAR(10) NOT NULL
        CHECK (result IN ('SUCCESS', 'FAILURE')),
    reason_code VARCHAR(40) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(512) NULL,
    request_id VARCHAR(64) NOT NULL,
    metadata JSONB NULL
);

CREATE INDEX ix_admin_audit_occurred_at
    ON admin_platform.admin_audit (occurred_at);

CREATE INDEX ix_admin_audit_actor_user_id
    ON admin_platform.admin_audit (actor_user_id);
