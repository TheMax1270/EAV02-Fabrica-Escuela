BEGIN;

CREATE SCHEMA IF NOT EXISTS moderation;

CREATE TABLE moderation.reports (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL REFERENCES users.app_users(id),
    target_type VARCHAR(20) NOT NULL,
    target_id UUID NOT NULL,
    reason VARCHAR(30) NOT NULL,
    details VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_reports_pending_per_reporter
    ON moderation.reports (reporter_id, target_type, target_id)
    WHERE status = 'PENDING';

CREATE INDEX ix_reports_target ON moderation.reports (target_type, target_id);

COMMIT;