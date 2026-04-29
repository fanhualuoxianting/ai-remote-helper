package com.airh.protocol.dto;

import com.airh.protocol.enums.TaskStatus;

import java.time.Instant;

public record TaskResult(
        String taskId,
        TaskStatus status,
        String summary,
        String output,
        String errorMessage,
        Instant finishedAt
) {
}
