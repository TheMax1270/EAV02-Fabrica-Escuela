BEGIN;

CREATE TABLE interactions.user_blocks (
    id UUID PRIMARY KEY,
    blocker_id UUID NOT NULL REFERENCES users.app_users(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES users.app_users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_user_blocks_not_self CHECK (blocker_id <> blocked_id),
    CONSTRAINT ux_user_blocks_pair UNIQUE (blocker_id, blocked_id)
);

CREATE INDEX ix_user_blocks_blocked ON interactions.user_blocks (blocked_id, created_at);

COMMIT;
