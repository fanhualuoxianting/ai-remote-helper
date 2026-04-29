package com.airh.relay.repository;

import com.airh.relay.domain.AuditEventEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuditEventRepositoryTest {
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void savesAuditEvent() {
        String eventId = UUID.randomUUID().toString();
        AuditEventEntity event = new AuditEventEntity(
                eventId,
                "session-1",
                "TASK_CREATED",
                "{\"taskId\":\"task-1\"}",
                Instant.now()
        );

        auditEventRepository.save(event);
        Optional<AuditEventEntity> saved = auditEventRepository.findById(eventId);

        assertThat(saved).isPresent();
        assertThat(saved.get().getSessionId()).isEqualTo("session-1");
        assertThat(saved.get().getEventType()).isEqualTo("TASK_CREATED");
        assertThat(saved.get().getDetail()).contains("task-1");
    }
}
