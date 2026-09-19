CREATE SCHEMA IF NOT EXISTS admin;

CREATE TABLE admin.audit_logs (
    id uuid PRIMARY KEY,
    admin_id uuid NOT NULL REFERENCES public.app_users(id),
    action varchar(100) NOT NULL,
    target_type varchar(50) NOT NULL,
    target_id uuid NOT NULL,
    details jsonb,
    created_at timestamptz NOT NULL
);

CREATE INDEX ix_audit_logs_admin_id ON admin.audit_logs (admin_id);
CREATE INDEX ix_audit_logs_target ON admin.audit_logs (target_type, target_id);
CREATE INDEX ix_audit_logs_created_at ON admin.audit_logs (created_at DESC);
