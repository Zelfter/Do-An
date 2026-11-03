-- Migration V010: Seed Default Admin User
-- FR-RBAC: Admin created via DB migration — no self-registration API for ADMIN role
-- NFR-17: password stored as bcrypt hash (pgcrypto crypt function)
-- design.md §2.1: admin email + password from environment variables
--
-- IMPORTANT: Set ADMIN_EMAIL and ADMIN_PASSWORD env vars before running migration.
-- The placeholder values below are ONLY for local development.
-- Production deployment MUST override via environment variables.
-- NEVER commit real passwords to version control (design.md §6, .gitignore).
--
-- Flyway placeholder substitution:
--   Spring Boot: set spring.flyway.placeholders.admin_email / .admin_password in application.yml
--                (populated from env vars ADMIN_EMAIL / ADMIN_PASSWORD).
--   Maven plugin: set <placeholders> in pom.xml (or -Dseed.admin.email / -Dseed.admin.password).
-- Flyway replaces dollar-brace tokens across the whole file (including comments),
-- so the syntax is a simple token with no inline default value. The dev defaults
-- (admin@example.com / ChangeMe123!) are supplied via the placeholder config above
-- (Spring Boot: spring.flyway.placeholders; Maven plugin: the <placeholders> block).
--
-- Alternative: run this INSERT manually after deploy with real bcrypt hash:
-- INSERT INTO users(email, password_hash, role, status)
--     VALUES ('admin@yourdomain.com', crypt('YourPassword', gen_salt('bf', 12)), 'ADMIN', 'ACTIVE');

INSERT INTO users (id, email, password_hash, role, status)
VALUES (
    gen_random_uuid(),
    -- Flyway placeholder admin_email (default admin@example.com from placeholder config)
    '${admin_email}',
    -- pgcrypto: bcrypt cost=12; password from Flyway placeholder admin_password
    crypt('${admin_password}', gen_salt('bf', 12)),
    'ADMIN',
    'ACTIVE'
)
ON CONFLICT (email) DO NOTHING;

-- Note: ON CONFLICT ensures idempotent re-run
-- Admin cannot be deleted via API — only via direct DB access (by design)
