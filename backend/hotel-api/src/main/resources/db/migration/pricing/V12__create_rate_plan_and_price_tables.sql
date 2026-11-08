-- Pricing module schema: rate plans, seasons, base/season prices and manual overrides.
-- This migration may only touch the pricing schema.

CREATE TABLE pricing.rate_plans (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name_tr VARCHAR(120) NOT NULL,
    name_en VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_rate_plans_code UNIQUE (code),
    CONSTRAINT chk_rate_plans_code CHECK (code ~ '^[A-Z][A-Z0-9_-]{1,49}$'),
    CONSTRAINT chk_rate_plans_name_tr CHECK (
        char_length(btrim(name_tr)) BETWEEN 1 AND 120
        AND name_tr !~ '[<>[:cntrl:]]'
    ),
    CONSTRAINT chk_rate_plans_name_en CHECK (
        char_length(btrim(name_en)) BETWEEN 1 AND 120
        AND name_en !~ '[<>[:cntrl:]]'
    )
);

CREATE UNIQUE INDEX ux_rate_plans_single_default
    ON pricing.rate_plans (is_default)
    WHERE is_default = TRUE;

CREATE TABLE pricing.seasons (
    id UUID PRIMARY KEY,
    rate_plan_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name_tr VARCHAR(120) NOT NULL,
    name_en VARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_seasons_rate_plans
        FOREIGN KEY (rate_plan_id) REFERENCES pricing.rate_plans (id) ON DELETE CASCADE,
    CONSTRAINT uq_seasons_plan_code UNIQUE (rate_plan_id, code),
    CONSTRAINT chk_seasons_code CHECK (code ~ '^[A-Z][A-Z0-9_-]{1,49}$'),
    CONSTRAINT chk_seasons_name_tr CHECK (
        char_length(btrim(name_tr)) BETWEEN 1 AND 120
        AND name_tr !~ '[<>[:cntrl:]]'
    ),
    CONSTRAINT chk_seasons_name_en CHECK (
        char_length(btrim(name_en)) BETWEEN 1 AND 120
        AND name_en !~ '[<>[:cntrl:]]'
    ),
    CONSTRAINT chk_seasons_period CHECK (start_date <= end_date)
);

CREATE INDEX ix_seasons_plan_dates
    ON pricing.seasons (rate_plan_id, start_date, end_date);

CREATE TABLE pricing.rate_plan_prices (
    id UUID PRIMARY KEY,
    rate_plan_id UUID NOT NULL,
    room_type_id UUID NOT NULL,
    guests SMALLINT NOT NULL,
    amount_try NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_rate_plan_prices_rate_plans
        FOREIGN KEY (rate_plan_id) REFERENCES pricing.rate_plans (id) ON DELETE CASCADE,
    CONSTRAINT uq_rate_plan_prices_plan_room_guests
        UNIQUE (rate_plan_id, room_type_id, guests),
    CONSTRAINT chk_rate_plan_prices_guests CHECK (guests >= 1),
    CONSTRAINT chk_rate_plan_prices_amount CHECK (amount_try > 0)
);

CREATE INDEX ix_rate_plan_prices_lookup
    ON pricing.rate_plan_prices (rate_plan_id, room_type_id, guests);

CREATE TABLE pricing.season_prices (
    id UUID PRIMARY KEY,
    season_id UUID NOT NULL,
    room_type_id UUID NOT NULL,
    guests SMALLINT NOT NULL,
    amount_try NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_season_prices_seasons
        FOREIGN KEY (season_id) REFERENCES pricing.seasons (id) ON DELETE CASCADE,
    CONSTRAINT uq_season_prices_season_room_guests
        UNIQUE (season_id, room_type_id, guests),
    CONSTRAINT chk_season_prices_guests CHECK (guests >= 1),
    CONSTRAINT chk_season_prices_amount CHECK (amount_try > 0)
);

CREATE INDEX ix_season_prices_lookup
    ON pricing.season_prices (season_id, room_type_id, guests);

CREATE TABLE pricing.manual_prices (
    id UUID PRIMARY KEY,
    rate_plan_id UUID NOT NULL,
    room_type_id UUID NOT NULL,
    guests SMALLINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    amount_try NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_manual_prices_rate_plans
        FOREIGN KEY (rate_plan_id) REFERENCES pricing.rate_plans (id) ON DELETE CASCADE,
    CONSTRAINT chk_manual_prices_guests CHECK (guests >= 1),
    CONSTRAINT chk_manual_prices_period CHECK (start_date <= end_date),
    CONSTRAINT chk_manual_prices_amount CHECK (amount_try > 0)
);

CREATE INDEX ix_manual_prices_lookup
    ON pricing.manual_prices (rate_plan_id, room_type_id, guests, start_date, end_date);
