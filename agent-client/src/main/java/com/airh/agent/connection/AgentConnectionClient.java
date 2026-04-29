package com.airh.agent.connection;

import com.airh.agent.executor.CommandExecutionService;
import com.airh.agent.executor.TaskExecutor;
import com.airh.agent.filesystem.FileSystemService;
import com.airh.agent.safety.PathSandbox;
import com.airh.protocol.dto.AgentHelloMessage;
import com.airh.protocol.dto.HeartbeatMessage;
import com.airh.protocol.dto.TaskLogMessage;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AgentConnectionClient {
    private final String deviceId;
    private final String deviceName;
    private final AgentConnectionListener listener;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "agent-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private ScheduledFuture<?> heartbeatTask;
    private TaskExecutor taskExecutor;
    private String sessionId;

    public AgentConnectionClient(String deviceId, String deviceName, AgentConnectionListener listener) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.listener = listener;
    }

    public void connect(String serverUrl, String authorizedDirectory) {
        if (isConnected()) {
            listener.onLog("当前已连接，忽略重复连接请求");
            return;
        }

        String websocketUrl = toWebSocketUrl(serverUrl);
        listener.onConnecting(websocketUrl);
        PathSandbox pathSandbox = new PathSandbox(Path.of(authorizedDirectory));
        taskExecutor = new TaskExecutor(new FileSystemService(pathSandbox), new CommandExecutionService(pathSandbox),
                (taskId, message) -> sendTaskLog(taskId, "INFO", message));

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        stompClient.connectAsync(websocketUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                stompSession = session;
                listener.onLog("STOMP 已连接，发送 Agent hello");
                session.subscribe("/topic/agent/" + deviceId + "/events", new ServerEventHandler());
                session.send("/app/agent/hello", new AgentHelloMessage(
                        UUID.randomUUID().toString(),
                        deviceId,
                        deviceName,
                        authorizedDirectory,
                        "0.1.0-SNAPSHOT",
                        Instant.now().toString()
                ));
                startHeartbeat();
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                listener.onLog("连接异常：" + exception.getMessage());
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                listener.onDisconnected("传输断开：" + exception.getMessage());
                stopHeartbeat();
            }
        });
    }

    public void disconnect() {
        stopHeartbeat();
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        stompSession = null;
        sessionId = null;
        if (taskExecutor != null) {
            taskExecutor.close();
            taskExecutor = null;
        }
        listener.onDisconnected("已手动断开");
    }

    public boolean isConnected() {
        return stompSession != null && stompSession.isConnected();
    }

    public void shutdown() {
        disconnect();
        heartbeatExecutor.shutdownNow();
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (isConnected()) {
                stompSession.send("/app/agent/heartbeat", new HeartbeatMessage(
                        UUID.randomUUID().toString(),
                        deviceId,
                        sessionId,
                        Instant.now().toString()
                ));
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
            heartbeatTask = null;
        }
    }

    private String toWebSocketUrl(String serverUrl) {
        URI uri = URI.create(serverUrl.strip());
        String scheme = switch (uri.getScheme()) {
            case "https" -> "wss";
            case "http" -> "ws";
            case "ws", "wss" -> uri.getScheme();
            default -> throw new IllegalArgumentException("服务器地址必须以 http://、https://、ws:// 或 wss:// 开头");
        };
        String authority = uri.getRawAuthority();
        String basePath = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "" : uri.getRawPath();
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        return scheme + "://" + authority + basePath + "/ws/agent";
    }

    private final class ServerEventHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return Map.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            if (!(payload instanceof Map<?, ?> event)) {
                listener.onLog("收到服务端消息：" + payload);
                return;
            }

            Object code = event.get("connectionCode");
            Object returnedSessionId = event.get("sessionId");
            Object status = event.get("status");
            if (code != null && returnedSessionId != null) {
                sessionId = returnedSessionId.toString();
                listener.onConnected(sessionId, code.toString());
                listener.onLog("收到服务端在线确认，状态：" + status);
                return;
            }

            Object errorCode = event.get("code");
            Object message = event.get("message");
            if (errorCode != null) {
                listener.onLog("收到服务端错误：" + errorCode + "，" + message);
                return;
            }

            Object taskId = event.get("taskId");
            Object taskType = event.get("taskType");
            if (taskId != null && taskType != null) {
                Object authorizedDirectory = event.get("authorizedDirectory");
                if (authorizedDirectory != null) {
                    listener.onLog("任务授权目录：" + authorizedDirectory);
                }
                handleTask(event, taskId.toString(), taskType.toString());
                return;
            }

            listener.onLog("收到服务端消息：" + event);
        }

        private void handleTask(Map<?, ?> task, String taskId, String taskType) {
            Object payload = task.get("payload");
            String payloadSummary = summarizePayload(payload);
            listener.onTaskStarted(taskId, taskType, payloadSummary);
            sendTaskLog(taskId, "INFO", "Agent 已收到任务：" + taskType + "，准备异步执行");
            if (taskExecutor == null) {
                sendTaskLog(taskId, "ERROR", "任务执行器未初始化");
                return;
            }

            taskExecutor.execute(taskId, sessionId, taskType, payload)
                    .thenAccept(result -> {
                        if (isConnected()) {
                            stompSession.send("/app/agent/task-result", result);
                        }
                        listener.onTaskOutput(taskId, result.output());
                        listener.onTaskFinished(taskId, result.status().name(), result.summary());
                    });
        }

        private String summarizePayload(Object payload) {
            if (payload == null) {
                return "{}";
            }
            String text = payload.toString();
            return text.length() <= 160 ? text : text.substring(0, 160) + "...";
        }
    }

    private void sendTaskLog(String taskId, String level, String message) {
        if (isConnected()) {
            stompSession.send("/app/agent/task-log", new TaskLogMessage(
                    taskId,
                    sessionId,
                    level,
                    message,
                    Instant.now()
            ));
        }
        listener.onLog("任务日志已上报：" + message);
    }
}
