ALTER TABLE media.media_public_cleanup_tasks
    ALTER COLUMN attempts TYPE INTEGER;

ALTER TABLE media.media_public_cleanup_tasks
    ADD COLUMN dead_lettered_at TIMESTAMPTZ;
