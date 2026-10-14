-- V9: publicaciones y discusiones tecnicas (HU-06, Task 53)
BEGIN;

CREATE SCHEMA IF NOT EXISTS content;

CREATE TABLE content.posts (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES users.app_users(id),
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200),
    description VARCHAR(5000) NOT NULL,
    link_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_posts_type
        CHECK (type IN ('GENERAL_UPDATE', 'TECHNICAL_DISCUSSION')),
    CONSTRAINT ck_posts_status
        CHECK (status IN ('OPEN', 'RESOLVED')),
    CONSTRAINT ck_posts_description_not_blank
        CHECK (length(btrim(description)) > 0),
    CONSTRAINT ck_posts_discussion_title
        CHECK (type <> 'TECHNICAL_DISCUSSION'
               OR (title IS NOT NULL AND length(btrim(title)) > 0)),
    CONSTRAINT ck_posts_resolved_only_discussion
        CHECK (type = 'TECHNICAL_DISCUSSION' OR status = 'OPEN'),
    CONSTRAINT ck_posts_resolved_at
        CHECK ((status = 'RESOLVED') = (resolved_at IS NOT NULL)),
    CONSTRAINT ck_posts_dates
        CHECK (updated_at >= created_at)
);

CREATE TABLE content.tags (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    CONSTRAINT ck_tags_name_normalized
        CHECK (name <> '' AND name = lower(btrim(name)))
);

CREATE TABLE content.post_tags (
    post_id UUID NOT NULL REFERENCES content.posts(id),
    tag_id UUID NOT NULL REFERENCES content.tags(id),
    PRIMARY KEY (post_id, tag_id)
);

CREATE INDEX ix_posts_author_id ON content.posts(author_id);
CREATE INDEX ix_posts_created_at ON content.posts(created_at DESC);
CREATE INDEX ix_post_tags_tag_id ON content.post_tags(tag_id);

COMMIT;