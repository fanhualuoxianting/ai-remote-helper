package com.airh.agent.executor;

import com.airh.agent.filesystem.FileSystemService;
import com.airh.protocol.dto.TaskResultMessage;
import com.airh.protocol.enums.TaskStatus;
import com.airh.protocol.enums.TaskType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class TaskExecutor implements AutoCloseable {
    private final FileSystemService fileSystemService;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;
    private final BiConsumer<String, String> taskLogSink;

    public TaskExecutor(FileSystemService fileSystemService, BiConsumer<String, String> taskLogSink) {
        this.fileSystemService = Objects.requireNonNull(fileSystemService, "fileSystemService must not be null");
        this.taskLogSink = Objects.requireNonNull(taskLogSink, "taskLogSink must not be null");
        this.executorService = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "agent-task-executor");
            thread.setDaemon(true);
            return thread;
        });
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public CompletableFuture<TaskResultMessage> execute(String taskId, String sessionId, String taskType, Object payload) {
        return CompletableFuture.supplyAsync(() -> executeSync(taskId, sessionId, taskType, payload), executorService);
    }

    private TaskResultMessage executeSync(String taskId, String sessionId, String taskTypeText, Object payload) {
        try {
            TaskType taskType = TaskType.valueOf(normalizeTaskType(taskTypeText));
            String relativePath = extractRelativePath(payload);
            taskLogSink.accept(taskId, "开始执行任务：" + taskType + "，路径：" + relativePath);
            return switch (taskType) {
                case LIST_DIR -> executeListDirectory(taskId, sessionId, relativePath);
                case READ_FILE -> executeReadFile(taskId, sessionId, relativePath);
                default -> unsupported(taskId, sessionId, taskType);
            };
        } catch (Exception exception) {
            taskLogSink.accept(taskId, "任务执行失败：" + exception.getMessage());
            return new TaskResultMessage(taskId, sessionId, TaskStatus.FAILED,
                    "任务执行失败", "", "", exception.getMessage(), Instant.now());
        }
    }

    private TaskResultMessage executeListDirectory(String taskId, String sessionId, String relativePath) throws JsonProcessingException, java.io.IOException {
        var entries = fileSystemService.listDirectory(relativePath);
        taskLogSink.accept(taskId, "目录读取完成，共 " + entries.size() + " 项");
        return new TaskResultMessage(taskId, sessionId, TaskStatus.SUCCESS,
                "目录列表读取成功，共 " + entries.size() + " 项",
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entries),
                "", null, Instant.now());
    }

    private TaskResultMessage executeReadFile(String taskId, String sessionId, String relativePath) throws JsonProcessingException, java.io.IOException {
        var result = fileSystemService.readFile(relativePath);
        String summary = result.contentReturned()
                ? "文件读取成功：" + result.name() + "，" + result.size() + " bytes"
                : "文件信息读取成功：" + result.name() + "，未返回正文";
        taskLogSink.accept(taskId, summary);
        return new TaskResultMessage(taskId, sessionId, TaskStatus.SUCCESS, summary,
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result),
                "", null, Instant.now());
    }

    private TaskResultMessage unsupported(String taskId, String sessionId, TaskType taskType) {
        String message = "Phase 05 只支持 LIST_DIR 和 READ_FILE，当前任务暂不执行：" + taskType;
        taskLogSink.accept(taskId, message);
        return new TaskResultMessage(taskId, sessionId, TaskStatus.FAILED,
                "不支持的任务类型", "", "", message, Instant.now());
    }

    private String extractRelativePath(Object payload) {
        if (!(payload instanceof Map<?, ?> payloadMap)) {
            return ".";
        }
        Object data = payloadMap.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            return firstText(dataMap, "path", "relativePath", "relative_path");
        }
        return firstText(payloadMap, "path", "relativePath", "relative_path");
    }

    private String firstText(Map<?, ?> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return ".";
    }

    private String normalizeTaskType(String taskTypeText) {
        if (taskTypeText == null || taskTypeText.isBlank()) {
            throw new IllegalArgumentException("taskType 不能为空");
        }
        return taskTypeText.trim().replace('-', '_').toUpperCase(java.util.Locale.ROOT);
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }
}
