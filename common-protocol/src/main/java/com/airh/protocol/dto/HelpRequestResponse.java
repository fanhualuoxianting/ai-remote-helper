package com.airh.protocol.dto;

import com.airh.protocol.enums.HelpRequestStatus;

import java.time.Instant;

public record HelpRequestResponse(
        String requestId,
        String sessionId,
        String content,
        HelpRequestStatus status,
        String reviewerNote,
        Instant createdAt,
        Instant updatedAt,
        Instant reviewedAt
) {
}
