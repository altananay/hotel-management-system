-- =============================================================================
-- Local & Testcontainers PostgreSQL Initialization (Single Source of Truth)
-- Creates:
--   Roles: migrator (DDL owner), app (DML runtime user, no DDL)
--   Databases: hotel_core (hotel-api), hotel_admin (admin-api)
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

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO migrator;
GRANT USAGE ON SCHEMA public TO app;

ALTER DEFAULT PRIVILEGES FOR ROLE migrator
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app;
ALTER DEFAULT PRIVILEGES FOR ROLE migrator
    GRANT USAGE, SELECT ON SEQUENCES TO app;

ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app;
ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO app;

\connect hotel_admin

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO migrator;
GRANT USAGE ON SCHEMA public TO app;

ALTER DEFAULT PRIVILEGES FOR ROLE migrator
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app;
ALTER DEFAULT PRIVILEGES FOR ROLE migrator
    GRANT USAGE, SELECT ON SEQUENCES TO app;

ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app;
ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO app;
