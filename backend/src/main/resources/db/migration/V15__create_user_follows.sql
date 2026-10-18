BEGIN;

CREATE TABLE interactions.user_follows (
    id UUID PRIMARY KEY,
    follower_id UUID NOT NULL REFERENCES users.app_users(id) ON DELETE CASCADE,
    followed_id UUID NOT NULL REFERENCES users.app_users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_user_follows_not_self CHECK (follower_id <> followed_id),
    CONSTRAINT ux_user_follows_pair UNIQUE (follower_id, followed_id)
);

CREATE INDEX ix_user_follows_followed ON interactions.user_follows (followed_id, created_at);

COMMIT;
