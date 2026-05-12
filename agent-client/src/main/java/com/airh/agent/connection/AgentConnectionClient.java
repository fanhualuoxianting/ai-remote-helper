package com.airh.agent.connection;

import com.airh.agent.executor.CommandExecutionService;
import com.airh.agent.executor.TaskExecutor;
import com.airh.agent.filesystem.FileSystemService;
import com.airh.agent.safety.PathSandbox;
import com.airh.protocol.dto.AgentHelloMessage;
import com.airh.protocol.dto.HeartbeatMessage;
import com.airh.protocol.dto.TaskLogMessage;
import com.airh.protocol.dto.TaskResultMessage;
import com.airh.protocol.enums.TaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    private static final String CLIENT_VERSION = "0.1.5";
    private static final int RECONNECT_DELAY_SECONDS = 3;

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
    private ScheduledFuture<?> reconnectTask;
    private TaskExecutor taskExecutor;
    private String sessionId;
    private String lastServerUrl;
    private String lastAuthorizedDirectory;
    private volatile boolean manualDisconnect = true;
    private volatile boolean connectionAttemptInFlight;

    public AgentConnectionClient(String deviceId, String deviceName, AgentConnectionListener listener) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.listener = listener;
    }

    public synchronized void connect(String serverUrl, String authorizedDirectory) {
        if (isConnected()) {
            listener.onLog("当前已连接，忽略重复连接请求");
            return;
        }
        lastServerUrl = serverUrl;
        lastAuthorizedDirectory = authorizedDirectory;
        manualDisconnect = false;
        connectionAttemptInFlight = false;
        cancelReconnect();
        doConnect(serverUrl, authorizedDirectory);
    }

    private synchronized void doConnect(String serverUrl, String authorizedDirectory) {
        if (connectionAttemptInFlight) {
            listener.onLog("连接请求正在进行中，等待当前尝试完成");
            return;
        }
        String websocketUrl = toWebSocketUrl(serverUrl);
        connectionAttemptInFlight = true;
        listener.onConnecting(websocketUrl);
        closeStaleResources();
        PathSandbox pathSandbox = new PathSandbox(Path.of(authorizedDirectory));
        taskExecutor = new TaskExecutor(new FileSystemService(pathSandbox), new CommandExecutionService(pathSandbox),
                pathSandbox.authorizedDirectory(),
                (taskId, message) -> sendTaskLog(taskId, "INFO", message));

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(buildMessageConverter());
        stompClient.connectAsync(websocketUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                connectionAttemptInFlight = false;
                stompSession = session;
                cancelReconnect();
                listener.onLog("STOMP 已连接，发送 Agent hello");
                session.subscribe("/topic/agent/" + deviceId + "/events", new ServerEventHandler());
                session.send("/app/agent/hello", new AgentHelloMessage(
                        UUID.randomUUID().toString(),
                        deviceId,
                        deviceName,
                        authorizedDirectory,
                        CLIENT_VERSION,
                        Instant.now().toString()
                ));
                startHeartbeat();
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                connectionAttemptInFlight = false;
                listener.onLog("连接异常：" + exception.getMessage());
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                connectionAttemptInFlight = false;
                handleUnexpectedDisconnect("传输断开：" + exception.getMessage());
            }
        }).whenComplete((session, throwable) -> {
            if (throwable != null) {
                connectionAttemptInFlight = false;
                listener.onLog("连接 relay-server 失败：" + throwable.getMessage());
                scheduleReconnect();
            }
        });
    }

    public synchronized void disconnect() {
        manualDisconnect = true;
        connectionAttemptInFlight = false;
        cancelReconnect();
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

    private synchronized void handleUnexpectedDisconnect(String reason) {
        stopHeartbeat();
        closeStaleResources();
        sessionId = null;
        listener.onDisconnected(reason + "；Agent 将自动重连");
        scheduleReconnect();
    }

    private synchronized void scheduleReconnect() {
        if (manualDisconnect || reconnectTask != null) {
            return;
        }
        reconnectTask = heartbeatExecutor.scheduleWithFixedDelay(() -> {
            if (manualDisconnect || isConnected()) {
                cancelReconnect();
                return;
            }
            if (connectionAttemptInFlight) {
                return;
            }
            if (lastServerUrl == null || lastAuthorizedDirectory == null) {
                return;
            }
            try {
                listener.onLog("正在自动重连 relay-server...");
                doConnect(lastServerUrl, lastAuthorizedDirectory);
            } catch (RuntimeException exception) {
                listener.onLog("自动重连失败：" + exception.getMessage());
            }
        }, RECONNECT_DELAY_SECONDS, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private synchronized void cancelReconnect() {
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
            reconnectTask = null;
        }
    }

    private synchronized void closeStaleResources() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        stompSession = null;
        if (taskExecutor != null) {
            taskExecutor.close();
            taskExecutor = null;
        }
        if (stompClient != null) {
            stompClient.stop();
            stompClient = null;
        }
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
        if ("ws".equals(scheme) || "wss".equals(scheme)) {
            return scheme + "://" + authority + (basePath.isBlank() ? "/ws" : basePath);
        }
        return scheme + "://" + authority + basePath + "/ws";
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
                            sendTaskResult(result);
                        }
                        listener.onTaskOutput(taskId, result.output());
                        listener.onTaskFinished(taskId, result.status().name(), result.summary());
                    })
                    .exceptionally(throwable -> {
                        String message = throwable.getCause() == null ? throwable.getMessage() : throwable.getCause().getMessage();
                        listener.onLog("任务执行链异常：" + message);
                        sendTaskResult(new TaskResultMessage(
                                taskId,
                                sessionId,
                                TaskStatus.FAILED,
                                "Agent 任务执行链异常",
                                "",
                                "",
                                message == null ? "未知异常" : message,
                                Instant.now()
                        ));
                        listener.onTaskFinished(taskId, TaskStatus.FAILED.name(), message == null ? "未知异常" : message);
                        return null;
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
        try {
            if (isConnected()) {
                stompSession.send("/app/agent/task-log", new TaskLogMessage(
                        taskId,
                        sessionId,
                        level,
                        message,
                        Instant.now()
                ));
            }
        } catch (RuntimeException exception) {
            listener.onLog("任务日志上报失败：" + exception.getMessage());
        }
        listener.onLog("任务日志已上报：" + message);
    }

    private void sendTaskResult(TaskResultMessage result) {
        try {
            if (isConnected()) {
                stompSession.send("/app/agent/task-result", result);
            }
        } catch (RuntimeException exception) {
            listener.onLog("任务结果上报失败：" + exception.getMessage());
        }
    }

    private MappingJackson2MessageConverter buildMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        return converter;
    }
}
