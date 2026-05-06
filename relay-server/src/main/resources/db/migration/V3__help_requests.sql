CREATE TABLE IF NOT EXISTS help_requests (
    request_id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    reviewer_note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_help_requests_session_id ON help_requests (session_id);
CREATE INDEX IF NOT EXISTS idx_help_requests_status ON help_requests (status);
CREATE INDEX IF NOT EXISTS idx_help_requests_created_at ON help_requests (created_at);
