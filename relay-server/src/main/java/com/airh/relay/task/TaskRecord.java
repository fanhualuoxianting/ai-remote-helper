package com.airh.relay.task;

import com.airh.protocol.dto.TaskPayload;
import com.airh.protocol.enums.TaskStatus;
import com.airh.protocol.enums.TaskType;

import java.time.Instant;

public record TaskRecord(
        String taskId,
        String sessionId,
        TaskType taskType,
        TaskPayload payload,
        TaskStatus status,
        int timeoutSeconds,
        String summary,
        String output,
        String stderr,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
    public TaskRecord withStatus(TaskStatus newStatus, Instant updatedAt) {
        return new TaskRecord(taskId, sessionId, taskType, payload, newStatus, timeoutSeconds,
                summary, output, stderr, errorMessage, createdAt, updatedAt, completedAt);
    }

    public TaskRecord withResult(TaskStatus newStatus, String resultSummary, String stdout, String resultStderr,
                                 String resultErrorMessage, Instant updatedAt) {
        return new TaskRecord(taskId, sessionId, taskType, payload, newStatus, timeoutSeconds,
                resultSummary, stdout, resultStderr, resultErrorMessage, createdAt, updatedAt, updatedAt);
    }
}
