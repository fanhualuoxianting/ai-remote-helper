package com.airh.protocol.dto;

public record AgentOnlineMessage(
        String messageId,
        String deviceId,
        String sessionId,
        String connectionCode,
        String status,
        String createdAt
) {
}
