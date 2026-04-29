package com.airh.relay.task;

import com.airh.protocol.dto.CreateTaskRequest;
import com.airh.protocol.dto.CreateTaskResponse;
import com.airh.protocol.dto.RemoteTask;
import com.airh.protocol.dto.TaskLog;
import com.airh.protocol.dto.TaskLogMessage;
import com.airh.protocol.dto.TaskPayload;
import com.airh.protocol.dto.TaskResultMessage;
import com.airh.protocol.enums.PermissionType;
import com.airh.protocol.enums.RiskLevel;
import com.airh.protocol.enums.TaskStatus;
import com.airh.relay.device.DeviceConnection;
import com.airh.relay.device.DeviceRegistry;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TaskService {
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final DeviceRegistry deviceRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentMap<String, TaskRecord> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<TaskLog>> logsByTaskId = new ConcurrentHashMap<>();

    public TaskService(DeviceRegistry deviceRegistry, SimpMessagingTemplate messagingTemplate) {
        this.deviceRegistry = deviceRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    public CreateTaskResponse createAndDispatch(String sessionId, CreateTaskRequest request) {
        DeviceConnection connection = deviceRegistry.findOnlineBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("未找到在线 Agent session：" + sessionId));
        if (request.taskType() == null) {
            throw new IllegalArgumentException("taskType 不能为空");
        }

        String taskId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        int timeoutSeconds = request.timeoutSeconds() == null || request.timeoutSeconds() <= 0
                ? DEFAULT_TIMEOUT_SECONDS
                : request.timeoutSeconds();
        TaskPayload payload = request.payload() == null ? new TaskPayload(java.util.Map.of()) : request.payload();
        TaskRecord pending = new TaskRecord(taskId, sessionId, request.taskType(), payload, TaskStatus.PENDING,
                timeoutSeconds, null, null, null, null, now, now);
        tasks.put(taskId, pending);
        appendLog(taskId, sessionId, "INFO", "relay-server 创建任务并准备下发给 Agent");

        RemoteTask taskMessage = new RemoteTask(
                taskId,
                sessionId,
                request.taskType(),
                TaskStatus.PENDING,
                RiskLevel.LOW,
                List.of(PermissionType.VIEW_LOGS),
                connection.authorizedDirectory(),
                payload,
                now,
                now.plusSeconds(timeoutSeconds)
        );
        tasks.put(taskId, pending.withStatus(TaskStatus.RUNNING, Instant.now()));
        appendLog(taskId, sessionId, "INFO", "relay-server 已通过 STOMP 下发任务");
        messagingTemplate.convertAndSend(agentTopic(connection.deviceId()), taskMessage);
        return new CreateTaskResponse(taskId, sessionId, TaskStatus.RUNNING);
    }

    public TaskRecord getTask(String taskId) {
        TaskRecord task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在：" + taskId);
        }
        return task;
    }

    public List<TaskLog> getLogs(String taskId) {
        if (!tasks.containsKey(taskId)) {
            throw new IllegalArgumentException("任务不存在：" + taskId);
        }
        return List.copyOf(logsByTaskId.getOrDefault(taskId, new CopyOnWriteArrayList<>()));
    }

    public void receiveTaskLog(TaskLogMessage message) {
        if (message.taskId() == null || !tasks.containsKey(message.taskId())) {
            return;
        }
        appendLog(message.taskId(), message.sessionId(), safeLevel(message.level()), message.message());
    }

    public void receiveTaskResult(TaskResultMessage message) {
        TaskRecord task = tasks.get(message.taskId());
        if (task == null) {
            return;
        }
        Instant finishedAt = message.finishedAt() == null ? Instant.now() : message.finishedAt();
        TaskStatus status = message.status() == null ? TaskStatus.FAILED : message.status();
        TaskRecord updated = task.withResult(status, message.summary(), message.output(), message.stderr(),
                message.errorMessage(), finishedAt);
        tasks.put(message.taskId(), updated);
        appendLog(message.taskId(), task.sessionId(), "INFO", "relay-server 已收到 Agent 模拟任务结果：" + status);
    }

    private void appendLog(String taskId, String sessionId, String level, String message) {
        logsByTaskId.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>())
                .add(new TaskLog(taskId, sessionId, level, message, Instant.now()));
    }

    private String safeLevel(String level) {
        return level == null || level.isBlank() ? "INFO" : level;
    }

    private String agentTopic(String deviceId) {
        return "/topic/agent/" + deviceId + "/events";
    }
}
