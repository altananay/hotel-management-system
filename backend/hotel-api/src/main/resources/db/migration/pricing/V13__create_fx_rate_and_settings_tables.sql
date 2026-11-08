-- Pricing module FX history and global settings.
-- This migration may only touch the pricing schema.

CREATE TABLE pricing.fx_rates (
    id UUID PRIMARY KEY,
    currency VARCHAR(3) NOT NULL,
    source VARCHAR(10) NOT NULL,
    rate_date DATE NOT NULL,
    rate NUMERIC(18,6) NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_fx_rates_currency_source_date UNIQUE (currency, source, rate_date),
    CONSTRAINT chk_fx_rates_currency CHECK (currency IN ('EUR', 'USD')),
    CONSTRAINT chk_fx_rates_source CHECK (source IN ('TCMB', 'MANUAL')),
    CONSTRAINT chk_fx_rates_rate CHECK (rate > 0)
);

CREATE INDEX ix_fx_rates_currency_source_date
    ON pricing.fx_rates (currency, source, rate_date DESC);

CREATE TABLE pricing.price_settings (
    id SMALLINT PRIMARY KEY,
    buffer_percent NUMERIC(5,2) NOT NULL,
    rounding_step NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_price_settings_singleton CHECK (id = 1),
    CONSTRAINT chk_price_settings_buffer CHECK (buffer_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_price_settings_rounding_step CHECK (rounding_step > 0)
);
