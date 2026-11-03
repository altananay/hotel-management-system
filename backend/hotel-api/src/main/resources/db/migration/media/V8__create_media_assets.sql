CREATE TABLE media.media_assets (
    id UUID PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    width INTEGER,
    height INTEGER,
    room_type_id UUID,
    published_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    alt_text_tr VARCHAR(200),
    alt_text_en VARCHAR(200),
    failure_code VARCHAR(100),
    processing_attempts SMALLINT NOT NULL DEFAULT 0,
    processing_started_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_media_assets_type CHECK (type IN ('PHOTO', 'PANORAMA')),
    CONSTRAINT chk_media_assets_status CHECK (status IN ('UPLOADED', 'PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT chk_media_assets_size CHECK (size_bytes > 0),
    CONSTRAINT chk_media_assets_width CHECK (width IS NULL OR width > 0),
    CONSTRAINT chk_media_assets_height CHECK (height IS NULL OR height > 0),
    CONSTRAINT chk_media_assets_display_order CHECK (display_order >= 0),
    CONSTRAINT chk_media_assets_attempts CHECK (processing_attempts >= 0),
    CONSTRAINT chk_media_assets_alt_tr CHECK (
        alt_text_tr IS NULL
        OR (char_length(btrim(alt_text_tr)) <= 200 AND alt_text_tr !~ '[<>[:cntrl:]]')
    ),
    CONSTRAINT chk_media_assets_alt_en CHECK (
        alt_text_en IS NULL
        OR (char_length(btrim(alt_text_en)) <= 200 AND alt_text_en !~ '[<>[:cntrl:]]')
    ),
    CONSTRAINT ux_media_assets_storage_key UNIQUE (storage_key)
);

CREATE TABLE media.media_variants (
    id UUID PRIMARY KEY,
    media_asset_id UUID NOT NULL,
    variant VARCHAR(20) NOT NULL,
    format VARCHAR(10) NOT NULL,
    private_storage_key VARCHAR(512) NOT NULL,
    public_storage_key VARCHAR(512),
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_media_variants_media_assets
        FOREIGN KEY (media_asset_id) REFERENCES media.media_assets (id) ON DELETE CASCADE,
    CONSTRAINT chk_media_variants_variant CHECK (variant IN ('THUMB', 'CARD', 'COVER')),
    CONSTRAINT chk_media_variants_format CHECK (format IN ('WEBP', 'JPEG')),
    CONSTRAINT chk_media_variants_width CHECK (width > 0),
    CONSTRAINT chk_media_variants_height CHECK (height > 0),
    CONSTRAINT chk_media_variants_size CHECK (size_bytes > 0),
    CONSTRAINT ux_media_variants_private_key UNIQUE (private_storage_key),
    CONSTRAINT ux_media_variants_public_key UNIQUE (public_storage_key),
    CONSTRAINT ux_media_variants_asset_variant_format
        UNIQUE (media_asset_id, variant, format)
);

CREATE INDEX ix_media_assets_room_type
    ON media.media_assets (room_type_id);

CREATE INDEX ix_media_assets_status_published
    ON media.media_assets (status, published_at);

CREATE UNIQUE INDEX ux_media_assets_room_order
    ON media.media_assets (room_type_id, display_order)
    WHERE archived_at IS NULL AND room_type_id IS NOT NULL;

CREATE UNIQUE INDEX ux_media_assets_room_cover
    ON media.media_assets (room_type_id)
    WHERE archived_at IS NULL AND published_at IS NOT NULL AND is_cover = TRUE;

CREATE INDEX ix_media_variants_asset
    ON media.media_variants (media_asset_id);
