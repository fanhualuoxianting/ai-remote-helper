package com.airh.relay.controller;

import com.airh.relay.device.DeviceConnection;
import com.airh.relay.device.DeviceRegistry;
import com.airh.relay.task.TaskRecord;
import com.airh.relay.task.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/console")
@CrossOrigin(origins = "*")
public class WebConsoleController {
    private final DeviceRegistry deviceRegistry;
    private final TaskService taskService;

    public WebConsoleController(DeviceRegistry deviceRegistry, TaskService taskService) {
        this.deviceRegistry = deviceRegistry;
        this.taskService = taskService;
    }

    @GetMapping("/devices")
    public ResponseEntity<List<DeviceConnection>> getDevices() {
        return ResponseEntity.ok(deviceRegistry.onlineDevices());
    }

    @GetMapping("/devices/{sessionId}")
    public ResponseEntity<DeviceConnection> getDevice(@PathVariable("sessionId") String sessionId) {
        return deviceRegistry.findOnlineBySessionId(sessionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<TaskRecord> getTask(@PathVariable("taskId") String taskId) {
        TaskRecord task = taskService.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "devices", deviceRegistry.onlineDevices().size()
        ));
    }
}
