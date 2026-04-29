package com.airh.protocol.dto;

import java.time.Instant;

public record TaskLogMessage(
        String taskId,
        String sessionId,
        String level,
        String message,
        Instant createdAt
) {
}
