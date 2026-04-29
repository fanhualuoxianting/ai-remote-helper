package com.airh.protocol.dto;

import com.airh.protocol.enums.MessageType;

import java.time.Instant;
import java.util.Map;

public record RemoteMessage(
        String messageId,
        MessageType type,
        String sessionId,
        Instant createdAt,
        Map<String, String> metadata
) {
}
