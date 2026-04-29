package com.airh.relay.service;

import com.airh.relay.domain.AuditEventEntity;
import com.airh.relay.repository.AuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    public void logEvent(String sessionId, String eventType, Map<String, ?> detail) {
        auditEventRepository.save(new AuditEventEntity(
                UUID.randomUUID().toString(),
                sessionId,
                eventType,
                toJson(detail),
                Instant.now()
        ));
    }

    private String toJson(Map<String, ?> detail) {
        try {
            return objectMapper.writeValueAsString(detail == null ? Map.of() : detail);
        } catch (JsonProcessingException e) {
            return "{\"serializationError\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
}
