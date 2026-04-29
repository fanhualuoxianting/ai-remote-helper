package com.airh.relay.session;

import com.airh.relay.device.DeviceConnection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
public class SessionStateCache {
    private static final Duration SESSION_TTL = Duration.ofMinutes(5);
    private static final String KEY_PREFIX = "airh:session:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public SessionStateCache(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void cacheOnline(DeviceConnection connection) {
        write(connection.sessionId(), Map.of(
                "deviceId", connection.deviceId(),
                "deviceName", nullToEmpty(connection.deviceName()),
                "stompSessionId", connection.stompSessionId(),
                "connectionCode", connection.connectionCode(),
                "authorizedDirectory", nullToEmpty(connection.authorizedDirectory()),
                "online", "true",
                "connectedAt", connection.connectedAt(),
                "lastHeartbeatAt", connection.lastHeartbeatAt()
        ));
    }

    public void cacheOffline(DeviceConnection connection) {
        write(connection.sessionId(), Map.of(
                "deviceId", connection.deviceId(),
                "online", "false",
                "lastHeartbeatAt", connection.lastHeartbeatAt()
        ));
    }

    public Optional<String> getSessionState(String sessionId) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key(sessionId)));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private void write(String sessionId, Map<String, String> state) {
        try {
            redisTemplate.opsForValue().set(key(sessionId), objectMapper.writeValueAsString(state), SESSION_TTL);
        } catch (JsonProcessingException | RuntimeException e) {
            // Redis is a cache layer; relay-server must keep serving existing in-memory session state.
        }
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
