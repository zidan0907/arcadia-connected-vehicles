-- ============================================================================
-- File: arcadia-app/src/main/resources/db/migration/V1__init_auth_schema.sql
-- Module: arcadia-auth
-- Description: Creates auth schema, user_accounts table (Sign-Up / Sign-In),
--              refresh_tokens table (durable session management), indexes,
--              and initial pilot accounts (Password: Arcadia@2026)
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS auth;

-- 1. User Accounts Table (Populated via Sign-Up API)
CREATE TABLE IF NOT EXISTS auth.user_accounts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             VARCHAR(64)  NOT NULL UNIQUE,
    username            VARCHAR(100) NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    full_name           VARCHAR(150) NOT NULL,
    role                VARCHAR(50)  NOT NULL DEFAULT 'ROLE_DRIVER',
    assigned_vin        VARCHAR(32),
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked  BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_auth_role CHECK (role IN ('ROLE_DRIVER', 'ROLE_FLEET_ADMIN'))
);

CREATE INDEX IF NOT EXISTS idx_user_accounts_email        ON auth.user_accounts (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_user_accounts_username     ON auth.user_accounts (LOWER(username));
CREATE INDEX IF NOT EXISTS idx_user_accounts_user_id      ON auth.user_accounts (user_id);
CREATE INDEX IF NOT EXISTS idx_user_accounts_assigned_vin ON auth.user_accounts (assigned_vin);

-- 2. Refresh Token Sessions Table (Tracks active/revoked refresh tokens in Postgres)
CREATE TABLE IF NOT EXISTS auth.refresh_tokens (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_jti           VARCHAR(64)  NOT NULL UNIQUE,
    user_id             VARCHAR(64)  NOT NULL,
    expires_at          TIMESTAMPTZ  NOT NULL,
    revoked             BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at          TIMESTAMPTZ,
    user_agent          VARCHAR(255),
    ip_address          VARCHAR(64),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES auth.user_accounts (user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_jti     ON auth.refresh_tokens (token_jti);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON auth.refresh_tokens (user_id);

-- ============================================================================
-- Seed Pilot Users (Default plaintext password for all accounts: Arcadia@2026)
-- BCrypt Hash (cost=10): $2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a
-- ============================================================================
INSERT INTO auth.user_accounts (
    id, user_id, username, email, password_hash, full_name, role, assigned_vin, enabled, account_non_locked
) VALUES
(
    '11111111-1111-1111-1111-111111111042',
    'user-1042',
    'driver.1042',
    'driver1042@arcadia.com',
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a',
    'Rohan Verma',
    'ROLE_DRIVER',
    'MAKZ1234567890',
    TRUE,
    TRUE
),
(
    '11111111-1111-1111-1111-111111119021',
    'user-9021',
    'alex.chen',
    'alex.chen@arcadia.com',
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a',
    'Alex Chen',
    'ROLE_DRIVER',
    'MAKZ9988776655',
    TRUE,
    TRUE
),
(
    '11111111-1111-1111-1111-111111110001',
    'admin-0001',
    'fleet.admin',
    'admin@arcadia.com',
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a',
    'Arcadia Fleet Admin',
    'ROLE_FLEET_ADMIN',
    NULL,
    TRUE,
    TRUE
)
ON CONFLICT (user_id) DO NOTHING;