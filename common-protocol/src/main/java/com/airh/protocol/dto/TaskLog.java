package com.airh.protocol.dto;

import java.time.Instant;

public record TaskLog(
        String taskId,
        String sessionId,
        String level,
        String message,
        Instant createdAt
) {
}
