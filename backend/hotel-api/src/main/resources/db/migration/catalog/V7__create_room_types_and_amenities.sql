CREATE TABLE catalog.amenities (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name_tr VARCHAR(80) NOT NULL,
    name_en VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_amenities_code UNIQUE (code),
    CONSTRAINT chk_amenities_code CHECK (code ~ '^[a-z][a-z0-9_]{1,49}$'),
    CONSTRAINT chk_amenities_name_tr CHECK (
        char_length(btrim(name_tr)) BETWEEN 1 AND 80
        AND name_tr !~ '[<>\u0001-\u001F\u007F-\u009F]'
    ),
    CONSTRAINT chk_amenities_name_en CHECK (
        char_length(btrim(name_en)) BETWEEN 1 AND 80
        AND name_en !~ '[<>\u0001-\u001F\u007F-\u009F]'
    )
);

CREATE TABLE catalog.room_types (
    id UUID PRIMARY KEY,
    name_tr VARCHAR(120) NOT NULL,
    name_en VARCHAR(120) NOT NULL,
    description_tr VARCHAR(2000) NOT NULL,
    description_en VARCHAR(2000) NOT NULL,
    max_guests SMALLINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_room_types_max_guests CHECK (max_guests BETWEEN 1 AND 20),
    CONSTRAINT chk_room_types_display_order CHECK (display_order >= 0),
    CONSTRAINT chk_room_types_name_tr CHECK (
        char_length(btrim(name_tr)) BETWEEN 1 AND 120
        AND name_tr !~ '[<>\u0001-\u001F\u007F-\u009F]'
    ),
    CONSTRAINT chk_room_types_name_en CHECK (
        char_length(btrim(name_en)) BETWEEN 1 AND 120
        AND name_en !~ '[<>\u0001-\u001F\u007F-\u009F]'
    ),
    CONSTRAINT chk_room_types_description_tr CHECK (
        char_length(btrim(description_tr)) BETWEEN 1 AND 2000
        AND description_tr !~ '[<>\u0001-\u001F\u007F-\u009F]'
    ),
    CONSTRAINT chk_room_types_description_en CHECK (
        char_length(btrim(description_en)) BETWEEN 1 AND 2000
        AND description_en !~ '[<>\u0001-\u001F\u007F-\u009F]'
    )
);

CREATE TABLE catalog.room_type_amenities (
    room_type_id UUID NOT NULL,
    amenity_id UUID NOT NULL,
    PRIMARY KEY (room_type_id, amenity_id),
    CONSTRAINT fk_room_type_amenities_room_types
        FOREIGN KEY (room_type_id) REFERENCES catalog.room_types (id) ON DELETE CASCADE,
    CONSTRAINT fk_room_type_amenities_amenities
        FOREIGN KEY (amenity_id) REFERENCES catalog.amenities (id) ON DELETE RESTRICT
);

CREATE INDEX ix_room_types_active_display_order_id
    ON catalog.room_types (active, display_order ASC, id ASC);

CREATE INDEX ix_room_type_amenities_amenity_id
    ON catalog.room_type_amenities (amenity_id);
