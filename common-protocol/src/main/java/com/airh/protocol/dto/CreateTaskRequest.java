package com.airh.protocol.dto;

import com.airh.protocol.enums.TaskType;

public record CreateTaskRequest(
        TaskType taskType,
        TaskPayload payload,
        Integer timeoutSeconds
) {
}
