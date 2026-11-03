-- Migration V001: Users and Refresh Tokens
-- FR-01 (Auth), FR-01-JWT (Refresh token), FR-RBAC (roles), FR-34 (user management)
-- NFR-17 (bcrypt), NFR-19 (RBAC), design.md §2.1

-- Enable uuid-ossp for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable pgcrypto for bcrypt (used in V010 seed)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CANDIDATE', 'ADMIN')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LOCKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

COMMENT ON TABLE users IS 'FR-01, NFR-17, NFR-19: User accounts with bcrypt password_hash and RBAC roles';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hash with cost ≥12 (Spring Security default) — NFR-17';
COMMENT ON COLUMN users.role IS 'CANDIDATE or ADMIN — NFR-19';
COMMENT ON COLUMN users.status IS 'ACTIVE or LOCKED — FR-34';

-- Refresh Tokens table
-- FR-01-JWT: server-side refresh token storage for single-use, revocable tokens
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

COMMENT ON TABLE refresh_tokens IS 'FR-01-JWT: server-side refresh token storage (SHA-256 of raw token)';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hash of raw refresh_token; raw token only sent to client once';
COMMENT ON COLUMN refresh_tokens.revoked IS 'Set TRUE when user locked (FR-34, AC-USER-01) or when token consumed (single-use)';
