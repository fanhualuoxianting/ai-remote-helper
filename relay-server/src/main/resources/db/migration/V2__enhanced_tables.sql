-- V2: 增强表结构

-- 文件修改记录表
CREATE TABLE IF NOT EXISTS file_changes (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(36),
    file_path TEXT NOT NULL,
    backup_path TEXT,
    before_hash VARCHAR(64),
    after_hash VARCHAR(64),
    change_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_file_changes_session_id ON file_changes (session_id);
CREATE INDEX IF NOT EXISTS idx_file_changes_task_id ON file_changes (task_id);

-- 设备表
CREATE TABLE IF NOT EXISTS devices (
    id VARCHAR(64) PRIMARY KEY,
    device_name VARCHAR(256),
    status VARCHAR(32) NOT NULL DEFAULT 'OFFLINE',
    last_online_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_devices_status ON devices (status);

-- 会话表
CREATE TABLE IF NOT EXISTS sessions (
    id VARCHAR(64) PRIMARY KEY,
    device_id VARCHAR(64),
    workspace_root TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (device_id) REFERENCES devices(id)
);

CREATE INDEX IF NOT EXISTS idx_sessions_device_id ON sessions (device_id);
CREATE INDEX IF NOT EXISTS idx_sessions_status ON sessions (status);

-- 任务日志表
CREATE TABLE IF NOT EXISTS task_logs (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    stream_type VARCHAR(32),
    content TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (task_id) REFERENCES task_records(task_id)
);

CREATE INDEX IF NOT EXISTS idx_task_logs_task_id ON task_logs (task_id);
