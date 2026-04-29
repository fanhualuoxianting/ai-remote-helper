package com.airh.protocol.dto;

import com.airh.protocol.enums.TaskStatus;

public record CreateTaskResponse(
        String taskId,
        String sessionId,
        TaskStatus status
) {
}
