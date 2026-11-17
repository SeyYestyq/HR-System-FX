ALTER TABLE users ADD COLUMN IF NOT EXISTS uuid UUID UNIQUE DEFAULT gen_random_uuid();
ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

UPDATE users SET uuid = gen_random_uuid() WHERE uuid IS NULL;
ALTER TABLE users ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE users ALTER COLUMN uuid SET DEFAULT gen_random_uuid();

-- Disable legacy employer accounts (non-BCrypt hashes)
UPDATE users SET enabled = FALSE, role = 'VIEWER' WHERE role = 'EMPLOYER';
UPDATE users SET role = 'ADMIN', enabled = TRUE, must_change_password = TRUE WHERE role = 'ADMIN';
UPDATE users SET role = 'CANDIDATE' WHERE role = 'CANDIDATE';
UPDATE users SET role = 'VIEWER', enabled = FALSE WHERE role = 'CANDIDATE';

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('VIEWER', 'EDITOR', 'ADMIN'));

-- Expand password_hash for BCrypt
ALTER TABLE users ALTER COLUMN password_hash TYPE VARCHAR(100);

CREATE TABLE IF NOT EXISTS refresh_sessions (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_agent VARCHAR(500),
    ip_address VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_refresh_user ON refresh_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_expires ON refresh_sessions(expires_at);

CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    actor_email VARCHAR(255),
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80),
    entity_uuid UUID,
    description VARCHAR(1000) NOT NULL,
    correlation_id VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details_json TEXT
);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_events(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_type ON audit_events(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_events(entity_type, entity_uuid);

-- Link vacancy_notes.author_id to users
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'vacancy_notes_author_id_fkey'
    ) THEN
        -- Ensure at least one user exists as author for existing notes
        ALTER TABLE vacancy_notes
            ADD CONSTRAINT vacancy_notes_author_id_fkey
            FOREIGN KEY (author_id) REFERENCES users(id);
    END IF;
END $$;
