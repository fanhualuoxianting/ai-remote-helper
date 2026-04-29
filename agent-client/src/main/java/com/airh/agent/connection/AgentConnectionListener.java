package com.airh.agent.connection;

public interface AgentConnectionListener {
    void onConnecting(String serverUrl);

    void onConnected(String sessionId, String connectionCode);

    void onDisconnected(String reason);

    void onTaskStarted(String taskId, String taskType, String payloadSummary);

    void onTaskFinished(String taskId, String status, String summary);

    void onTaskOutput(String taskId, String output);

    void onLog(String message);
}
