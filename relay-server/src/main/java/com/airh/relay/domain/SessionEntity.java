package com.airh.relay.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sessions")
public class SessionEntity {
    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "workspace_root")
    private String workspaceRoot;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    public SessionEntity() {}

    public SessionEntity(String id, String deviceId, String workspaceRoot) {
        this.id = id;
        this.deviceId = deviceId;
        this.workspaceRoot = workspaceRoot;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public String getWorkspaceRoot() { return workspaceRoot; }
    public String getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEndedAt() { return endedAt; }

    public void end() {
        this.status = "ENDED";
        this.endedAt = Instant.now();
    }
}
