package com.airh.protocol.dto;

public record HeartbeatMessage(
        String messageId,
        String deviceId,
        String sessionId,
        String createdAt
) {
}
