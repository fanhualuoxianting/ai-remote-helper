package com.airh.relay.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "task_logs")
public class TaskLogEntity {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "stream_type", length = 32)
    private String streamType;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public TaskLogEntity() {}

    public TaskLogEntity(String id, String taskId, String streamType, String content) {
        this.id = id;
        this.taskId = taskId;
        this.streamType = streamType;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getTaskId() { return taskId; }
    public String getStreamType() { return streamType; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
