package com.airh.relay.controller;

import com.airh.protocol.dto.CreateTaskRequest;
import com.airh.protocol.dto.CreateTaskResponse;
import com.airh.protocol.dto.TaskLog;
import com.airh.relay.task.TaskRecord;
import com.airh.relay.task.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/sessions/{sessionId}/tasks")
    public CreateTaskResponse createTask(@PathVariable("sessionId") String sessionId, @RequestBody CreateTaskRequest request) {
        return taskService.createAndDispatch(sessionId, request);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskRecord getTask(@PathVariable("taskId") String taskId) {
        return taskService.getTask(taskId);
    }

    @GetMapping("/tasks/{taskId}/logs")
    public List<TaskLog> getTaskLogs(@PathVariable("taskId") String taskId) {
        return taskService.getLogs(taskId);
    }
}
