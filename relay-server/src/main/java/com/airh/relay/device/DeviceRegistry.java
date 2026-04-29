package com.airh.relay.device;

import com.airh.protocol.dto.AgentHelloMessage;
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

    private final ConcurrentMap<String, DeviceConnection> devicesById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> deviceIdByStompSessionId = new ConcurrentHashMap<>();

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
        return connection;
    }

    public Optional<DeviceConnection> heartbeat(String deviceId, String stompSessionId) {
        DeviceConnection connection = devicesById.get(deviceId);
        if (connection == null || !connection.stompSessionId().equals(stompSessionId)) {
            return Optional.empty();
        }
        DeviceConnection updated = connection.withHeartbeat(Instant.now().toString());
        devicesById.put(deviceId, updated);
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
        return Optional.of(offline);
    }

    public List<DeviceConnection> onlineDevices() {
        return devicesById.values().stream()
                .filter(DeviceConnection::online)
                .sorted(Comparator.comparing(DeviceConnection::connectedAt))
                .toList();
    }

    private String generateConnectionCode() {
        int number = RANDOM.nextInt(1_000_000);
        return "%06d".formatted(number);
    }
}
