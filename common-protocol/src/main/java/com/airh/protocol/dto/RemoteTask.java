package com.airh.protocol.dto;

import com.airh.protocol.enums.PermissionType;
import com.airh.protocol.enums.RiskLevel;
import com.airh.protocol.enums.TaskStatus;
import com.airh.protocol.enums.TaskType;

import java.time.Instant;
import java.util.List;

public record RemoteTask(
        String taskId,
        String sessionId,
        TaskType taskType,
        TaskStatus status,
        RiskLevel riskLevel,
        List<PermissionType> requiredPermissions,
        String authorizedDirectory,
        TaskPayload payload,
        Instant createdAt,
        Instant expiresAt
) {
}
