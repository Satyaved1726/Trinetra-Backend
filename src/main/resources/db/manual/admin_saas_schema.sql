-- Admin SaaS schema additions for PostgreSQL/Supabase
-- Run this script before deploying the new admin APIs when ddl-auto is disabled.

ALTER TABLE complaints
    ADD COLUMN IF NOT EXISTS priority varchar(20) NOT NULL DEFAULT 'MEDIUM';

ALTER TABLE complaints
    ADD COLUMN IF NOT EXISTS assigned_to varchar(120) NULL;

ALTER TABLE complaints
    ADD COLUMN IF NOT EXISTS status_history jsonb NOT NULL DEFAULT '[]'::jsonb;

UPDATE complaints
SET status_history = '[]'::jsonb
WHERE status_history IS NULL OR jsonb_typeof(status_history) = 'string';

ALTER TABLE complaints
    ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone NOT NULL DEFAULT now();

ALTER TABLE complaints
    DROP CONSTRAINT IF EXISTS complaints_status_check;

ALTER TABLE complaints
    ADD CONSTRAINT complaints_status_check
    CHECK (status IN ('SUBMITTED', 'PENDING', 'UNDER_REVIEW', 'INVESTIGATING', 'RESOLVED', 'REJECTED'));

CREATE TABLE IF NOT EXISTS complaint_comments (
    id uuid PRIMARY KEY,
    complaint_id uuid NOT NULL,
    user_id uuid NULL,
    comment text NOT NULL,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT fk_complaint_comments_complaint
        FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
);

ALTER TABLE complaint_comments
    ADD COLUMN IF NOT EXISTS comment text;

ALTER TABLE complaint_comments
    ADD COLUMN IF NOT EXISTS user_id uuid;

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id uuid PRIMARY KEY,
    complaint_id uuid NULL,
    action_type varchar(100) NOT NULL,
    action_details text NULL,
    actor varchar(255) NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_complaints_assigned_to
    ON complaints (assigned_to);

CREATE INDEX IF NOT EXISTS idx_complaints_priority_created
    ON complaints (priority, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_complaint_evidence_files_complaint_uploaded
    ON complaint_evidence_files (complaint_id, uploaded_at DESC);

CREATE INDEX IF NOT EXISTS idx_complaint_comments_complaint_created
    ON complaint_comments (complaint_id, created_at DESC);
