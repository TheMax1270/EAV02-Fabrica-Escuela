BEGIN;

ALTER TABLE posts.posts
    ADD COLUMN moderation_status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    ADD CONSTRAINT ck_posts_moderation_status
        CHECK (moderation_status IN ('VISIBLE', 'IN_REVIEW', 'REMOVED'));

ALTER TABLE interactions.comments
    ADD COLUMN moderation_status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    ADD CONSTRAINT ck_comments_moderation_status
        CHECK (moderation_status IN ('VISIBLE', 'IN_REVIEW', 'REMOVED'));

ALTER TABLE profiles.developer_profiles
    ADD COLUMN moderation_status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    ADD CONSTRAINT ck_profiles_moderation_status
        CHECK (moderation_status IN ('VISIBLE', 'IN_REVIEW', 'REMOVED'));

CREATE INDEX ix_posts_moderation_status ON posts.posts (moderation_status);
CREATE INDEX ix_comments_moderation_status ON interactions.comments (moderation_status);
CREATE INDEX ix_profiles_moderation_status ON profiles.developer_profiles (moderation_status);

COMMIT;
