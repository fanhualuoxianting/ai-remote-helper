package com.airh.relay.websocket;

import com.airh.protocol.dto.AgentHelloMessage;
import com.airh.protocol.dto.AgentOnlineMessage;
import com.airh.protocol.dto.ErrorMessage;
import com.airh.protocol.dto.HeartbeatMessage;
import com.airh.relay.device.DeviceConnection;
import com.airh.relay.device.DeviceRegistry;
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

    public AgentConnectionController(DeviceRegistry deviceRegistry, SimpMessagingTemplate messagingTemplate) {
        this.deviceRegistry = deviceRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/agent/hello")
    public void hello(AgentHelloMessage message, SimpMessageHeaderAccessor headers) {
        String stompSessionId = headers.getSessionId();
        if (message.deviceId() == null || message.deviceId().isBlank() || stompSessionId == null) {
            publishError(message.deviceId(), "INVALID_AGENT_HELLO", "deviceId 或 STOMP session 缺失");
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
