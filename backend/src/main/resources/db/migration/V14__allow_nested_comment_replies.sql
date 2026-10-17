BEGIN;

ALTER TABLE interactions.comments
    DROP CONSTRAINT fk_comments_parent_same_target_root;

ALTER TABLE interactions.comments
    DROP COLUMN parent_must_be_root;

ALTER TABLE interactions.comments
    ADD CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_comment_id)
        REFERENCES interactions.comments(id)
        ON DELETE NO ACTION;

COMMIT;