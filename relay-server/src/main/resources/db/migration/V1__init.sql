CREATE TABLE IF NOT EXISTS task_records (
    task_id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payload TEXT,
    summary TEXT,
    output TEXT,
    stderr TEXT,
    error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS audit_events (
    event_id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(64),
    event_type VARCHAR(64) NOT NULL,
    detail TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_task_records_session_id ON task_records (session_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_session_id ON audit_events (session_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_created_at ON audit_events (created_at);
