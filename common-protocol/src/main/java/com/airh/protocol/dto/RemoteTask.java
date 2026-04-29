package com.airh.protocol.dto;

import com.airh.protocol.enums.PermissionType;
import com.airh.protocol.enums.RiskLevel;
import com.airh.protocol.enums.TaskStatus;
import com.airh.protocol.enums.TaskType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RemoteTask(
        String taskId,
        String sessionId,
        TaskType type,
        TaskStatus status,
        RiskLevel riskLevel,
        List<PermissionType> requiredPermissions,
        Map<String, String> parameters,
        Instant createdAt,
        Instant expiresAt
) {
}
