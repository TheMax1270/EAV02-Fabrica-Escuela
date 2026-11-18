BEGIN;

CREATE SCHEMA IF NOT EXISTS notifications;

CREATE TABLE notifications.notifications (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES users.app_users(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users.app_users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id UUID NOT NULL,
    target_label VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    CONSTRAINT ck_notifications_type CHECK (type IN ('COMMENT', 'LIKE', 'FOLLOWER', 'MESSAGE')),
    CONSTRAINT ck_notifications_target_type CHECK (target_type IN ('POST', 'PROJECT', 'USER')),
    CONSTRAINT ck_notifications_not_self CHECK (recipient_id <> actor_id)
);

CREATE INDEX ix_notifications_recipient_created
    ON notifications.notifications (recipient_id, created_at DESC, id DESC);

CREATE INDEX ix_notifications_recipient_unread
    ON notifications.notifications (recipient_id) WHERE read_at IS NULL;

-- Un "me gusta" o un seguidor sin leer no se duplica aunque el usuario repita la acción.
CREATE UNIQUE INDEX ux_notifications_unread_dedupe
    ON notifications.notifications (recipient_id, actor_id, type, target_type, target_id)
    WHERE read_at IS NULL AND type IN ('LIKE', 'FOLLOWER');

COMMIT;