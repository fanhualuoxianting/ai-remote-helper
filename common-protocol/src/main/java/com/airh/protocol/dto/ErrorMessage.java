package com.airh.protocol.dto;

public record ErrorMessage(
        String messageId,
        String code,
        String message,
        String createdAt
) {
}
