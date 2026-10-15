CREATE SCHEMA IF NOT EXISTS interactions;

CREATE TABLE interactions.comments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users.app_users(id) ON DELETE NO ACTION,
    project_id UUID REFERENCES projects.projects(id) ON DELETE NO ACTION,
    post_id UUID REFERENCES posts.posts(id) ON DELETE NO ACTION,
    parent_comment_id UUID,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    target_type TEXT GENERATED ALWAYS AS (
        CASE WHEN project_id IS NOT NULL THEN 'PROJECT' ELSE 'POST' END
    ) STORED,
    target_id UUID GENERATED ALWAYS AS (COALESCE(project_id, post_id)) STORED,
    is_root BOOLEAN GENERATED ALWAYS AS (parent_comment_id IS NULL) STORED,
    parent_must_be_root BOOLEAN GENERATED ALWAYS AS (TRUE) STORED,
    CONSTRAINT ck_comments_target CHECK (
        (project_id IS NOT NULL AND post_id IS NULL)
        OR (project_id IS NULL AND post_id IS NOT NULL)
    ),
    CONSTRAINT ck_comments_content_not_blank CHECK (content ~ '[^[:space:]]'),
    CONSTRAINT ck_comments_parent_not_self CHECK (parent_comment_id IS NULL OR parent_comment_id <> id),
    CONSTRAINT uq_comments_identity_target_root UNIQUE (id, target_type, target_id, is_root),
    CONSTRAINT fk_comments_parent_same_target_root
        FOREIGN KEY (parent_comment_id, target_type, target_id, parent_must_be_root)
        REFERENCES interactions.comments(id, target_type, target_id, is_root)
        ON DELETE NO ACTION
);

CREATE INDEX ix_comments_user_id ON interactions.comments(user_id);
CREATE INDEX ix_comments_project_id ON interactions.comments(project_id) WHERE project_id IS NOT NULL;
CREATE INDEX ix_comments_post_id ON interactions.comments(post_id) WHERE post_id IS NOT NULL;
CREATE INDEX ix_comments_project_roots ON interactions.comments(project_id, created_at, id)
    WHERE project_id IS NOT NULL AND parent_comment_id IS NULL;
CREATE INDEX ix_comments_post_roots ON interactions.comments(post_id, created_at, id)
    WHERE post_id IS NOT NULL AND parent_comment_id IS NULL;
CREATE INDEX ix_comments_parent_created ON interactions.comments(parent_comment_id, created_at, id)
    WHERE parent_comment_id IS NOT NULL;

CREATE TABLE interactions.reactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users.app_users(id) ON DELETE NO ACTION,
    project_id UUID REFERENCES projects.projects(id) ON DELETE NO ACTION,
    post_id UUID REFERENCES posts.posts(id) ON DELETE NO ACTION,
    type VARCHAR(10) NOT NULL DEFAULT 'LIKE',
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_reactions_type CHECK (type = 'LIKE'),
    CONSTRAINT ck_reactions_target CHECK (
        (project_id IS NOT NULL AND post_id IS NULL)
        OR (project_id IS NULL AND post_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX ux_reactions_project_user_type
    ON interactions.reactions(project_id, user_id, type) WHERE project_id IS NOT NULL;
CREATE UNIQUE INDEX ux_reactions_post_user_type
    ON interactions.reactions(post_id, user_id, type) WHERE post_id IS NOT NULL;
CREATE INDEX ix_reactions_user_id ON interactions.reactions(user_id);
