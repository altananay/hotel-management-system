CREATE TABLE media.media_public_cleanup_tasks (
    id UUID PRIMARY KEY,
    public_storage_key VARCHAR(512) NOT NULL,
    attempts SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    last_attempt_at TIMESTAMPTZ,
    CONSTRAINT ux_media_public_cleanup_key UNIQUE (public_storage_key),
    CONSTRAINT chk_media_public_cleanup_attempts CHECK (attempts >= 0)
);

CREATE INDEX ix_media_public_cleanup_created
    ON media.media_public_cleanup_tasks (created_at);

GRANT SELECT, INSERT, UPDATE, DELETE
    ON media.media_public_cleanup_tasks TO app;
