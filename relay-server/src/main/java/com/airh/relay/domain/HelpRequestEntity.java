package com.airh.relay.domain;

import com.airh.protocol.enums.HelpRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "help_requests")
public class HelpRequestEntity {
    @Id
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private HelpRequestStatus status;

    @Column(name = "reviewer_note", columnDefinition = "text")
    private String reviewerNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected HelpRequestEntity() {
    }

    public HelpRequestEntity(String requestId, String sessionId, String content, HelpRequestStatus status,
                             String reviewerNote, Instant createdAt, Instant updatedAt, Instant reviewedAt) {
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.content = content;
        this.status = status;
        this.reviewerNote = reviewerNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.reviewedAt = reviewedAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getContent() {
        return content;
    }

    public HelpRequestStatus getStatus() {
        return status;
    }

    public String getReviewerNote() {
        return reviewerNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void review(HelpRequestStatus newStatus, String note, Instant now) {
        this.status = newStatus;
        this.reviewerNote = note;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    public void markAiState(HelpRequestStatus newStatus, String note, Instant now) {
        this.status = newStatus;
        this.reviewerNote = note;
        this.updatedAt = now;
    }
}
