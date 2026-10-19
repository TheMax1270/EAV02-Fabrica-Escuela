ALTER TABLE posts.posts
    ADD COLUMN type VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN link VARCHAR(500),
    ADD COLUMN categories TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    ADD COLUMN resolved BOOLEAN NOT NULL DEFAULT FALSE,
    ALTER COLUMN title DROP NOT NULL;

ALTER TABLE posts.posts
    ADD CONSTRAINT ck_posts_type CHECK (type IN ('GENERAL', 'TECHNICAL_DISCUSSION')),
    ADD CONSTRAINT ck_posts_discussion_title CHECK (
        type <> 'TECHNICAL_DISCUSSION' OR (title IS NOT NULL AND title ~ '[^[:space:]]')
    ),
    ADD CONSTRAINT ck_posts_discussion_categories CHECK (
        type <> 'TECHNICAL_DISCUSSION' OR (
            cardinality(categories) >= 1 AND array_to_string(categories, '') ~ '[^[:space:]]'
        )
    ),
    ADD CONSTRAINT ck_posts_categories CHECK (
        cardinality(categories) <= 20
        AND array_position(categories, NULL) IS NULL
        AND array_position(categories, '') IS NULL
    ),
    ADD CONSTRAINT ck_posts_general_unresolved CHECK (type <> 'GENERAL' OR resolved = FALSE),
    ADD CONSTRAINT ck_posts_link CHECK (link IS NULL OR link ~ '^https?://[^[:space:]]+$');
