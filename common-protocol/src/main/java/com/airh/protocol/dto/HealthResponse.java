package com.airh.protocol.dto;

public record HealthResponse(
        String appName,
        String version,
        String serverTime,
        String websocketEndpoint,
        String status
) {
}
