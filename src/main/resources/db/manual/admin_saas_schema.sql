-- Admin SaaS schema additions for PostgreSQL/Supabase
-- Run this script before deploying the new admin APIs when ddl-auto is disabled.

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id uuid PRIMARY KEY,
    complaint_id uuid NULL,
    action_type varchar(60) NOT NULL,
    action_details varchar(2000) NULL,
    actor varchar(120) NOT NULL,
    created_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_access_control (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL UNIQUE,
    blocked boolean NOT NULL DEFAULT false,
    updated_by varchar(120) NULL,
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_access_control_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created_at
    ON admin_audit_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_complaint_created
    ON admin_audit_logs (complaint_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_access_control_user_id
    ON user_access_control (user_id);

CREATE INDEX IF NOT EXISTS idx_complaints_status_created
    ON complaints (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_complaints_category_created
    ON complaints (category, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_complaints_created_at
    ON complaints (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_complaint_evidence_files_complaint_uploaded
    ON complaint_evidence_files (complaint_id, uploaded_at DESC);
