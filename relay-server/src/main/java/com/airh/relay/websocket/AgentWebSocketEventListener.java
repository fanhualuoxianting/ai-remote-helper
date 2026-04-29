package com.airh.relay.websocket;

import com.airh.relay.device.DeviceRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class AgentWebSocketEventListener {
    private final DeviceRegistry deviceRegistry;

    public AgentWebSocketEventListener(DeviceRegistry deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        deviceRegistry.markOfflineByStompSessionId(event.getSessionId());
    }
}
