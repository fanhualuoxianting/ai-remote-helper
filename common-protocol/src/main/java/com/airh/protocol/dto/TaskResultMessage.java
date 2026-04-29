package com.airh.protocol.dto;

import com.airh.protocol.enums.TaskStatus;

import java.time.Instant;

public record TaskResultMessage(
        String taskId,
        String sessionId,
        TaskStatus status,
        String summary,
        String output,
        String stderr,
        String errorMessage,
        Instant finishedAt
) {
}
