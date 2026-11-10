BEGIN;

CREATE SCHEMA IF NOT EXISTS messaging;

-- Una conversacion por par de usuarios: el par se guarda ordenado (user_one_id < user_two_id).
CREATE TABLE messaging.conversations (
    id UUID PRIMARY KEY,
    user_one_id UUID NOT NULL REFERENCES users.app_users(id) ON DELETE CASCADE,
    user_two_id UUID NOT NULL REFERENCES users.app_users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    last_message_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_conversations_ordered_pair CHECK (user_one_id < user_two_id),
    CONSTRAINT ux_conversations_pair UNIQUE (user_one_id, user_two_id)
);

CREATE INDEX ix_conversations_user_one ON messaging.conversations (user_one_id, last_message_at DESC);
CREATE INDEX ix_conversations_user_two ON messaging.conversations (user_two_id, last_message_at DESC);

CREATE TABLE messaging.messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES messaging.conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users.app_users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_messages_content_not_blank CHECK (content ~ '[^[:space:]]'),
    CONSTRAINT ck_messages_content_length CHECK (char_length(content) <= 2000)
);

CREATE INDEX ix_messages_conversation_created ON messaging.messages (conversation_id, created_at DESC, id DESC);

COMMIT;
