package com.airh.relay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final ThreadPoolTaskScheduler stompHeartbeatTaskScheduler;

    public WebSocketConfig(ThreadPoolTaskScheduler stompHeartbeatTaskScheduler) {
        this.stompHeartbeatTaskScheduler = stompHeartbeatTaskScheduler;
    }

    @org.springframework.context.annotation.Bean
    public static ThreadPoolTaskScheduler stompHeartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/agent").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/controller").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic")
                .setTaskScheduler(stompHeartbeatTaskScheduler)
                .setHeartbeatValue(new long[]{10000, 10000});
    }
}
