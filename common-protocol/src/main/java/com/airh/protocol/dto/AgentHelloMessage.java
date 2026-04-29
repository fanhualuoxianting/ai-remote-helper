package com.airh.protocol.dto;

public record AgentHelloMessage(
        String messageId,
        String deviceId,
        String deviceName,
        String authorizedDirectory,
        String clientVersion,
        String createdAt
) {
}
