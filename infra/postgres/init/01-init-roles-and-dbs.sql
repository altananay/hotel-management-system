-- =============================================================================
-- Local & Testcontainers PostgreSQL Initialization (Single Source of Truth)
-- Creates:
--   Roles: migrator (DDL owner), app (DML runtime user, no DDL)
--   Databases: hotel_core (hotel-api), hotel_admin (admin-api)
-- Security model:
--   - public schema is closed to applications: only migrator gets USAGE+CREATE.
--     app must never reach public (Flyway schema history included).
--   - app DML grants are defined only in module schemas, by each module's own
--     migration (db/migration/<module>/V<n>__*.sql ->
--      ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA <module> ...).
--   - This file runs ONLY on an empty data volume; after changes reset with:
--     docker compose --env-file .env -f infra/docker-compose.yml down -v
-- =============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'migrator') THEN
        CREATE ROLE migrator WITH LOGIN PASSWORD 'migrator_local_dev';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app') THEN
        CREATE ROLE app WITH LOGIN PASSWORD 'app_local_dev';
    END IF;
END
$$;

CREATE DATABASE hotel_core OWNER migrator;
CREATE DATABASE hotel_admin OWNER migrator;

REVOKE ALL ON DATABASE hotel_core FROM PUBLIC;
GRANT CONNECT, CREATE ON DATABASE hotel_core TO migrator;
GRANT CONNECT ON DATABASE hotel_core TO app;

REVOKE ALL ON DATABASE hotel_admin FROM PUBLIC;
GRANT CONNECT, CREATE ON DATABASE hotel_admin TO migrator;
GRANT CONNECT ON DATABASE hotel_admin TO app;

\connect hotel_core

-- public schema: closed to everyone except migrator (Flyway history lives here).
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO migrator;

-- Local/CI-only seed guard marker. This database comment is intentionally absent
-- from production databases; db/seed/catalog/R__seed_catalog_dev.sql refuses to
-- run without it, even if Flyway's effective URL is overridden.
COMMENT ON DATABASE hotel_core IS 'local-ci-seed';

\connect hotel_admin

-- public schema: closed to everyone except migrator (Flyway history lives here).
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO migrator;
