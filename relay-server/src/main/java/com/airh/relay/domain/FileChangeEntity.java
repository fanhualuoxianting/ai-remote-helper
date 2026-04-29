package com.airh.relay.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "file_changes")
public class FileChangeEntity {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "task_id", length = 36)
    private String taskId;

    @Column(name = "file_path", nullable = false, columnDefinition = "text")
    private String filePath;

    @Column(name = "backup_path", columnDefinition = "text")
    private String backupPath;

    @Column(name = "before_hash", length = 64)
    private String beforeHash;

    @Column(name = "after_hash", length = 64)
    private String afterHash;

    @Column(name = "change_type", nullable = false, length = 32)
    private String changeType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public FileChangeEntity() {}

    public FileChangeEntity(String id, String sessionId, String taskId, String filePath,
                            String backupPath, String beforeHash, String afterHash, String changeType) {
        this.id = id;
        this.sessionId = sessionId;
        this.taskId = taskId;
        this.filePath = filePath;
        this.backupPath = backupPath;
        this.beforeHash = beforeHash;
        this.afterHash = afterHash;
        this.changeType = changeType;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getTaskId() { return taskId; }
    public String getFilePath() { return filePath; }
    public String getBackupPath() { return backupPath; }
    public String getBeforeHash() { return beforeHash; }
    public String getAfterHash() { return afterHash; }
    public String getChangeType() { return changeType; }
    public Instant getCreatedAt() { return createdAt; }
}
