package com.airh.relay.websocket;

import com.airh.protocol.dto.AgentHelloMessage;
import com.airh.protocol.dto.AgentOnlineMessage;
import com.airh.protocol.dto.ErrorMessage;
import com.airh.protocol.dto.HeartbeatMessage;
import com.airh.protocol.dto.TaskLogMessage;
import com.airh.protocol.dto.TaskResultMessage;
import com.airh.relay.device.DeviceConnection;
import com.airh.relay.device.DeviceRegistry;
import com.airh.relay.task.TaskService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.UUID;

@Controller
public class AgentConnectionController {
    private final DeviceRegistry deviceRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskService taskService;

    public AgentConnectionController(DeviceRegistry deviceRegistry, SimpMessagingTemplate messagingTemplate, TaskService taskService) {
        this.deviceRegistry = deviceRegistry;
        this.messagingTemplate = messagingTemplate;
        this.taskService = taskService;
    }

    @MessageMapping("/agent/hello")
    public void hello(AgentHelloMessage message, SimpMessageHeaderAccessor headers) {
        String stompSessionId = headers.getSessionId();
        if (message.deviceId() == null || message.deviceId().isBlank() || stompSessionId == null) {
            publishError(message.deviceId(), "INVALID_AGENT_HELLO", "deviceId 或 STOMP session 缺失");
            return;
        }
        if (message.authorizedDirectory() == null || message.authorizedDirectory().isBlank()) {
            publishError(message.deviceId(), "MISSING_AUTHORIZED_DIRECTORY", "缺少授权目录，拒绝连接注册");
            return;
        }

        DeviceConnection connection = deviceRegistry.register(message, stompSessionId);
        AgentOnlineMessage onlineMessage = new AgentOnlineMessage(
                UUID.randomUUID().toString(),
                connection.deviceId(),
                connection.sessionId(),
                connection.connectionCode(),
                "ONLINE",
                Instant.now().toString()
        );
        messagingTemplate.convertAndSend(agentTopic(connection.deviceId()), onlineMessage);
    }

    @MessageMapping("/agent/heartbeat")
    public void heartbeat(HeartbeatMessage message, SimpMessageHeaderAccessor headers) {
        String stompSessionId = headers.getSessionId();
        if (message.deviceId() == null || stompSessionId == null) {
            publishError(message.deviceId(), "INVALID_HEARTBEAT", "deviceId 或 STOMP session 缺失");
            return;
        }
        deviceRegistry.heartbeat(message.deviceId(), stompSessionId);
    }

    @MessageMapping("/agent/task-log")
    public void taskLog(TaskLogMessage message) {
        taskService.receiveTaskLog(message);
    }

    @MessageMapping("/agent/task-result")
    public void taskResult(TaskResultMessage message) {
        taskService.receiveTaskResult(message);
    }

    private void publishError(String deviceId, String code, String message) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        messagingTemplate.convertAndSend(agentTopic(deviceId), new ErrorMessage(
                UUID.randomUUID().toString(),
                code,
                message,
                Instant.now().toString()
        ));
    }

    private String agentTopic(String deviceId) {
        return "/topic/agent/" + deviceId + "/events";
    }
}
