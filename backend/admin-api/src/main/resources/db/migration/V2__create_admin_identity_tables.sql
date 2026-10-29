CREATE TABLE admin_platform.admin_users (
    id UUID PRIMARY KEY,
    username VARCHAR(40) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
        CHECK (role IN ('SUPER_ADMIN', 'MANAGER', 'FRONT_DESK')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_admin_users_username_lower
    ON admin_platform.admin_users (lower(username));

CREATE UNIQUE INDEX ux_admin_users_email_lower
    ON admin_platform.admin_users (lower(email));

CREATE TABLE admin_platform.admin_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL
        REFERENCES admin_platform.admin_users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    absolute_expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_admin_sessions_user_id
    ON admin_platform.admin_sessions (user_id);

CREATE TABLE admin_platform.admin_login_attempts (
    id UUID PRIMARY KEY,
    identifier_norm VARCHAR(254) NOT NULL,
    user_id UUID NULL,
    ip_address VARCHAR(45) NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL,
    locked_until TIMESTAMPTZ NULL
);

CREATE INDEX ix_admin_login_attempts_key_time
    ON admin_platform.admin_login_attempts (identifier_norm, ip_address, attempted_at);

CREATE INDEX ix_admin_login_attempts_locked_until
    ON admin_platform.admin_login_attempts (locked_until);
