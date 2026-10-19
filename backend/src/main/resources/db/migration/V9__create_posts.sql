CREATE SCHEMA IF NOT EXISTS posts;

CREATE TABLE posts.posts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    content VARCHAR(5000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users.app_users(id),
    CONSTRAINT ck_posts_title_not_blank CHECK (title ~ '[^[:space:]]'),
    CONSTRAINT ck_posts_content_not_blank CHECK (content ~ '[^[:space:]]')
);

CREATE INDEX ix_posts_user_id ON posts.posts(user_id);
CREATE INDEX ix_posts_created_at_id ON posts.posts(created_at DESC, id DESC);
