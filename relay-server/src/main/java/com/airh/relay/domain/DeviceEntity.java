package com.airh.relay.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "devices")
public class DeviceEntity {
    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "device_name", length = 256)
    private String deviceName;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "OFFLINE";

    @Column(name = "last_online_at")
    private Instant lastOnlineAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DeviceEntity() {}

    public DeviceEntity(String id, String deviceName) {
        this.id = id;
        this.deviceName = deviceName;
        this.status = "ONLINE";
        this.lastOnlineAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getDeviceName() { return deviceName; }
    public String getStatus() { return status; }
    public Instant getLastOnlineAt() { return lastOnlineAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void markOnline() {
        this.status = "ONLINE";
        this.lastOnlineAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markOffline() {
        this.status = "OFFLINE";
        this.updatedAt = Instant.now();
    }
}
