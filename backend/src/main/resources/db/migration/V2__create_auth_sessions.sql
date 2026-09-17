CREATE TABLE auth_sessions (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES app_users(id),
    refresh_token_hash varchar(64) NOT NULL UNIQUE,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    CONSTRAINT ck_auth_sessions_expiry CHECK (expires_at > created_at)
);

CREATE INDEX ix_auth_sessions_user_id ON auth_sessions (user_id);
CREATE INDEX ix_auth_sessions_expires_at ON auth_sessions (expires_at);
