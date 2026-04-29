package com.airh.relay.device;

import com.airh.protocol.dto.AgentHelloMessage;
import com.airh.relay.session.SessionStateCache;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class DeviceRegistry {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SessionStateCache sessionStateCache;
    private final ConcurrentMap<String, DeviceConnection> devicesById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> deviceIdByStompSessionId = new ConcurrentHashMap<>();

    public DeviceRegistry(SessionStateCache sessionStateCache) {
        this.sessionStateCache = sessionStateCache;
    }

    public DeviceConnection register(AgentHelloMessage helloMessage, String stompSessionId) {
        String now = Instant.now().toString();
        String sessionId = UUID.randomUUID().toString();
        String connectionCode = generateConnectionCode();
        DeviceConnection connection = new DeviceConnection(
                helloMessage.deviceId(),
                helloMessage.deviceName(),
                stompSessionId,
                sessionId,
                connectionCode,
                helloMessage.authorizedDirectory(),
                true,
                now,
                now
        );
        devicesById.put(helloMessage.deviceId(), connection);
        deviceIdByStompSessionId.put(stompSessionId, helloMessage.deviceId());
        sessionStateCache.cacheOnline(connection);
        return connection;
    }

    public Optional<DeviceConnection> heartbeat(String deviceId, String stompSessionId) {
        DeviceConnection connection = devicesById.get(deviceId);
        if (connection == null || !connection.stompSessionId().equals(stompSessionId)) {
            return Optional.empty();
        }
        DeviceConnection updated = connection.withHeartbeat(Instant.now().toString());
        devicesById.put(deviceId, updated);
        sessionStateCache.cacheOnline(updated);
        return Optional.of(updated);
    }

    public Optional<DeviceConnection> markOfflineByStompSessionId(String stompSessionId) {
        String deviceId = deviceIdByStompSessionId.remove(stompSessionId);
        if (deviceId == null) {
            return Optional.empty();
        }
        DeviceConnection connection = devicesById.get(deviceId);
        if (connection == null) {
            return Optional.empty();
        }
        DeviceConnection offline = connection.offline(Instant.now().toString());
        devicesById.put(deviceId, offline);
        sessionStateCache.cacheOffline(offline);
        return Optional.of(offline);
    }

    public List<DeviceConnection> onlineDevices() {
        return devicesById.values().stream()
                .filter(DeviceConnection::online)
                .sorted(Comparator.comparing(DeviceConnection::connectedAt))
                .toList();
    }

    public Optional<DeviceConnection> findOnlineBySessionId(String sessionId) {
        return devicesById.values().stream()
                .filter(DeviceConnection::online)
                .filter(connection -> connection.sessionId().equals(sessionId))
                .findFirst();
    }

    public Optional<String> getAuthorizedDirectory(String deviceId) {
        DeviceConnection connection = devicesById.get(deviceId);
        if (connection == null || !connection.online()) {
            return Optional.empty();
        }
        return Optional.ofNullable(connection.authorizedDirectory());
    }

    private String generateConnectionCode() {
        int number = RANDOM.nextInt(1_000_000);
        return "%06d".formatted(number);
    }
}
