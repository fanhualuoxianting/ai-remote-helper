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
import com.airh.relay.domain.TaskRecordEntity;
import com.airh.relay.repository.TaskRecordRepository;
import com.airh.relay.service.AuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TaskService {
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final Set<TaskStatus> ACTIVE_STATUSES = EnumSet.of(TaskStatus.PENDING, TaskStatus.RUNNING);

    private final DeviceRegistry deviceRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskRecordRepository taskRecordRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, TaskRecord> tasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<TaskLog>> logsByTaskId = new ConcurrentHashMap<>();

    public TaskService(DeviceRegistry deviceRegistry, SimpMessagingTemplate messagingTemplate,
                       TaskRecordRepository taskRecordRepository, AuditService auditService,
                       ObjectMapper objectMapper) {
        this.deviceRegistry = deviceRegistry;
        this.messagingTemplate = messagingTemplate;
        this.taskRecordRepository = taskRecordRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
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
                timeoutSeconds, null, null, null, null, now, now, null);
        tasks.put(taskId, pending);
        taskRecordRepository.save(toEntity(pending));
        auditService.logEvent(sessionId, "TASK_CREATED", Map.of(
                "taskId", taskId,
                "taskType", request.taskType().name(),
                "timeoutSeconds", String.valueOf(timeoutSeconds)
        ));
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
        TaskRecord running = pending.withStatus(TaskStatus.RUNNING, Instant.now());
        tasks.put(taskId, running);
        taskRecordRepository.save(toEntity(running));
        auditService.logEvent(sessionId, "TASK_DISPATCHED", Map.of(
                "taskId", taskId,
                "deviceId", connection.deviceId()
        ));
        appendLog(taskId, sessionId, "INFO", "relay-server 已通过 STOMP 下发任务");
        messagingTemplate.convertAndSend(agentTopic(connection.deviceId()), taskMessage);
        return new CreateTaskResponse(taskId, sessionId, TaskStatus.RUNNING);
    }

    public TaskRecord getTask(String taskId) {
        TaskRecord task = tasks.get(taskId);
        if (task != null) {
            expireTaskIfNeeded(task);
            return tasks.getOrDefault(taskId, task);
        }
        return taskRecordRepository.findById(taskId)
                .map(this::expirePersistedTaskIfNeeded)
                .map(this::toRecord)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在：" + taskId));
    }

    public List<TaskLog> getLogs(String taskId) {
        if (!tasks.containsKey(taskId) && !taskRecordRepository.existsById(taskId)) {
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
        if (task == null || !ACTIVE_STATUSES.contains(task.status())) {
            return;
        }
        Instant finishedAt = message.finishedAt() == null ? Instant.now() : message.finishedAt();
        TaskStatus status = message.status() == null ? TaskStatus.FAILED : message.status();
        TaskRecord updated = task.withResult(status, message.summary(), message.output(), message.stderr(),
                message.errorMessage(), finishedAt);
        tasks.put(message.taskId(), updated);
        taskRecordRepository.findById(message.taskId())
                .ifPresentOrElse(entity -> {
                    entity.complete(status, message.summary(), message.output(), message.stderr(),
                            message.errorMessage(), finishedAt);
                    taskRecordRepository.save(entity);
                }, () -> taskRecordRepository.save(toEntity(updated)));
        auditService.logEvent(task.sessionId(), "TASK_RESULT_RECEIVED", Map.of(
                "taskId", message.taskId(),
                "status", status.name()
        ));
        appendLog(message.taskId(), task.sessionId(), "INFO", "relay-server 已收到 Agent 真实任务结果：" + status);
    }

    @Scheduled(fixedDelay = 2000)
    public void sweepTimedOutTasks() {
        Instant now = Instant.now();
        tasks.values().forEach(task -> {
            if (ACTIVE_STATUSES.contains(task.status()) && task.createdAt().plusSeconds(task.timeoutSeconds()).isBefore(now)) {
                markTaskTimedOut(task, now, "任务超过 " + task.timeoutSeconds() + " 秒未完成，已由 relay-server 自动标记超时");
            }
        });

        Instant defaultCutoff = now.minusSeconds(DEFAULT_TIMEOUT_SECONDS);
        taskRecordRepository.findByStatusInAndCreatedAtBefore(ACTIVE_STATUSES, defaultCutoff)
                .forEach(entity -> expirePersistedTaskIfNeeded(entity, now));
    }

    private void appendLog(String taskId, String sessionId, String level, String message) {
        logsByTaskId.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>())
                .add(new TaskLog(taskId, sessionId, level, message, Instant.now()));
    }

    private String safeLevel(String level) {
        return level == null || level.isBlank() ? "INFO" : level;
    }

    private void expireTaskIfNeeded(TaskRecord task) {
        if (ACTIVE_STATUSES.contains(task.status())
                && task.createdAt().plusSeconds(task.timeoutSeconds()).isBefore(Instant.now())) {
            markTaskTimedOut(task, Instant.now(),
                    "任务超过 " + task.timeoutSeconds() + " 秒未完成，已由 relay-server 自动标记超时");
        }
    }

    private TaskRecordEntity expirePersistedTaskIfNeeded(TaskRecordEntity entity) {
        return expirePersistedTaskIfNeeded(entity, Instant.now());
    }

    private TaskRecordEntity expirePersistedTaskIfNeeded(TaskRecordEntity entity, Instant now) {
        if (!ACTIVE_STATUSES.contains(entity.getStatus())) {
            return entity;
        }
        if (!entity.getCreatedAt().plusSeconds(DEFAULT_TIMEOUT_SECONDS).isBefore(now)) {
            return entity;
        }
        String error = "任务超过 " + DEFAULT_TIMEOUT_SECONDS + " 秒未完成，已由 relay-server 自动标记超时";
        entity.complete(TaskStatus.TIMEOUT, "任务超时未返回结果", "", "", error, now);
        taskRecordRepository.save(entity);
        TaskRecord memoryTask = tasks.get(entity.getTaskId());
        if (memoryTask != null && ACTIVE_STATUSES.contains(memoryTask.status())) {
            tasks.put(entity.getTaskId(), memoryTask.withResult(TaskStatus.TIMEOUT,
                    "任务超时未返回结果", "", "", error, now));
        }
        appendLog(entity.getTaskId(), entity.getSessionId(), "WARN", error);
        auditService.logEvent(entity.getSessionId(), "TASK_TIMEOUT", Map.of("taskId", entity.getTaskId()));
        return entity;
    }

    private void markTaskTimedOut(TaskRecord task, Instant now, String error) {
        TaskRecord latest = tasks.get(task.taskId());
        if (latest == null || !ACTIVE_STATUSES.contains(latest.status())) {
            return;
        }
        TaskRecord timedOut = latest.withResult(TaskStatus.TIMEOUT, "任务超时未返回结果", "", "", error, now);
        tasks.put(latest.taskId(), timedOut);
        taskRecordRepository.findById(latest.taskId())
                .ifPresentOrElse(entity -> {
                    if (ACTIVE_STATUSES.contains(entity.getStatus())) {
                        entity.complete(TaskStatus.TIMEOUT, "任务超时未返回结果", "", "", error, now);
                        taskRecordRepository.save(entity);
                    }
                }, () -> taskRecordRepository.save(toEntity(timedOut)));
        appendLog(latest.taskId(), latest.sessionId(), "WARN", error);
        auditService.logEvent(latest.sessionId(), "TASK_TIMEOUT", Map.of("taskId", latest.taskId()));
    }

    private String agentTopic(String deviceId) {
        return "/topic/agent/" + deviceId + "/events";
    }

    private TaskRecordEntity toEntity(TaskRecord record) {
        return new TaskRecordEntity(
                record.taskId(),
                record.sessionId(),
                record.taskType(),
                record.status(),
                toJson(record.payload()),
                record.summary(),
                record.output(),
                record.stderr(),
                record.errorMessage(),
                record.createdAt(),
                record.updatedAt(),
                record.completedAt()
        );
    }

    private TaskRecord toRecord(TaskRecordEntity entity) {
        return new TaskRecord(
                entity.getTaskId(),
                entity.getSessionId(),
                entity.getTaskType(),
                fromJson(entity.getPayload()),
                entity.getStatus(),
                DEFAULT_TIMEOUT_SECONDS,
                entity.getSummary(),
                entity.getOutput(),
                entity.getStderr(),
                entity.getError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCompletedAt()
        );
    }

    private String toJson(TaskPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("任务 payload 序列化失败", e);
        }
    }

    private TaskPayload fromJson(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return new TaskPayload(Map.of());
        }
        try {
            return objectMapper.readValue(payloadJson, TaskPayload.class);
        } catch (JsonProcessingException e) {
            return new TaskPayload(Map.of("raw", payloadJson));
        }
    }
}
