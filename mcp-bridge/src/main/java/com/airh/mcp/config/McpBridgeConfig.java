package com.airh.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.bridge")
public class McpBridgeConfig {
    private String relayUrl = "ws://localhost:8080/ws";
    private String sessionId;
    private int timeoutSeconds = 60;

    public String getRelayUrl() { return relayUrl; }
    public void setRelayUrl(String relayUrl) { this.relayUrl = relayUrl; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
