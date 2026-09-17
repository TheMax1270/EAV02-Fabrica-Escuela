ALTER TABLE auth_sessions
ALTER COLUMN refresh_token_hash TYPE varchar(64);
