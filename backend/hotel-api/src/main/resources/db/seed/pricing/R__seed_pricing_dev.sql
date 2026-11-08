-- Repeatable development/CI seed for the pricing module.
-- Executed explicitly as migrator outside standard production Flyway locations.

-- Local/CI-only guard: the hotel_core database comment is set to 'local-ci-seed'
-- by infra/postgres/init on empty local/CI volumes and is intentionally absent in
-- production. The marker is read from the target database itself, so overriding
-- Flyway's effective URL (FLYWAY_URL, -Dflyway.url, config files, or initSql)
-- cannot apply seed data to prod.
SELECT 1 / CASE
    WHEN shobj_description(
        (SELECT oid FROM pg_database WHERE datname = current_database()),
        'pg_database'
    ) = 'local-ci-seed' THEN 1
    ELSE 0
END;

-- Defaults documented in .env.example; the application initializer also creates this row
-- when it is missing. Keeping it here makes local/CI deterministic for E2E.
INSERT INTO pricing.price_settings (
    id,
    buffer_percent,
    rounding_step,
    created_at,
    updated_at
)
VALUES (1, 2.00, 5.00, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- A deliberately stale TCMB rate (yesterday) so the admin screen can prove the stale-rate warning.
INSERT INTO pricing.fx_rates (
    id,
    currency,
    source,
    rate_date,
    rate,
    captured_at,
    created_at,
    updated_at
)
VALUES
    (
        '0199f2c0-0000-7000-8000-000000000601',
        'EUR',
        'TCMB',
        CURRENT_DATE - 1,
        42.500000,
        NOW(),
        NOW(),
        NOW()
    ),
    (
        '0199f2c0-0000-7000-8000-000000000602',
        'USD',
        'TCMB',
        CURRENT_DATE - 1,
        39.000000,
        NOW(),
        NOW(),
        NOW()
    )
ON CONFLICT (currency, source, rate_date) DO NOTHING;
