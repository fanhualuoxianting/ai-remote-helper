package com.airh.relay.device;

public record DeviceConnection(
        String deviceId,
        String deviceName,
        String stompSessionId,
        String sessionId,
        String connectionCode,
        String authorizedDirectory,
        boolean online,
        String connectedAt,
        String lastHeartbeatAt
) {
    public DeviceConnection withHeartbeat(String heartbeatAt) {
        return new DeviceConnection(
                deviceId,
                deviceName,
                stompSessionId,
                sessionId,
                connectionCode,
                authorizedDirectory,
                online,
                connectedAt,
                heartbeatAt
        );
    }

    public DeviceConnection offline(String disconnectedAt) {
        return new DeviceConnection(
                deviceId,
                deviceName,
                stompSessionId,
                sessionId,
                connectionCode,
                authorizedDirectory,
                false,
                connectedAt,
                disconnectedAt
        );
    }
}
