package com.airh.agent.connection;

public interface AgentConnectionListener {
    void onConnecting(String serverUrl);

    void onConnected(String sessionId, String connectionCode);

    void onDisconnected(String reason);

    void onLog(String message);
}
