package com.airh.mcp.bridge;

import com.airh.protocol.enums.TaskType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskBridge {
    private final Map<String, CompletableFuture<JsonNode>> pendingTasks = new ConcurrentHashMap<>();

    public String mapToolToTaskType(String toolName) {
        return switch (toolName) {
            case "list_directory" -> TaskType.LIST_DIR.name();
            case "read_file" -> TaskType.READ_FILE.name();
            case "write_file" -> TaskType.WRITE_FILE.name();
            case "apply_patch" -> TaskType.APPLY_PATCH.name();
            case "run_command" -> TaskType.RUN_COMMAND.name();
            default -> null;
        };
    }

    public CompletableFuture<JsonNode> createPendingTask(String taskId) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingTasks.put(taskId, future);
        return future;
    }

    public void completeTask(String taskId, JsonNode result) {
        CompletableFuture<JsonNode> future = pendingTasks.remove(taskId);
        if (future != null) {
            future.complete(result);
        }
    }

    public void failTask(String taskId, String error) {
        CompletableFuture<JsonNode> future = pendingTasks.remove(taskId);
        if (future != null) {
            future.completeExceptionally(new RuntimeException(error));
        }
    }
}
