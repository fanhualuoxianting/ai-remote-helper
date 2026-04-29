package com.airh.relay.domain;

import com.airh.protocol.enums.TaskStatus;
import com.airh.protocol.enums.TaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "task_records")
public class TaskRecordEntity {
    @Id
    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 64)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TaskStatus status;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "output", columnDefinition = "text")
    private String output;

    @Column(name = "stderr", columnDefinition = "text")
    private String stderr;

    @Column(name = "error", columnDefinition = "text")
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected TaskRecordEntity() {
    }

    public TaskRecordEntity(String taskId, String sessionId, TaskType taskType, TaskStatus status, String payload,
                            String summary, String output, String stderr, String error, Instant createdAt,
                            Instant updatedAt, Instant completedAt) {
        this.taskId = taskId;
        this.sessionId = sessionId;
        this.taskType = taskType;
        this.status = status;
        this.payload = payload;
        this.summary = summary;
        this.output = output;
        this.stderr = stderr;
        this.error = error;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getPayload() {
        return payload;
    }

    public String getSummary() {
        return summary;
    }

    public String getOutput() {
        return output;
    }

    public String getStderr() {
        return stderr;
    }

    public String getError() {
        return error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void markRunning(Instant updatedAt) {
        this.status = TaskStatus.RUNNING;
        this.updatedAt = updatedAt;
    }

    public void complete(TaskStatus status, String summary, String output, String stderr, String error,
                         Instant completedAt) {
        this.status = status;
        this.summary = summary;
        this.output = output;
        this.stderr = stderr;
        this.error = error;
        this.completedAt = completedAt;
        this.updatedAt = completedAt;
    }
}
