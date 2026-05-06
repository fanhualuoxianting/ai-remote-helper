package com.airh.relay.repository;

import com.airh.protocol.enums.HelpRequestStatus;
import com.airh.relay.domain.HelpRequestEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class HelpRequestRepositoryTest {
    @Autowired
    private HelpRequestRepository helpRequestRepository;

    @Test
    void savesAndFindsBySessionId() {
        Instant now = Instant.now();
        HelpRequestEntity entity = new HelpRequestEntity(
                UUID.randomUUID().toString(),
                "session-1",
                "请帮我修复启动失败",
                HelpRequestStatus.PENDING,
                null,
                now,
                now,
                null
        );

        helpRequestRepository.save(entity);

        assertThat(helpRequestRepository.findBySessionIdOrderByCreatedAtDesc("session-1"))
                .hasSize(1)
                .first()
                .extracting(HelpRequestEntity::getStatus)
                .isEqualTo(HelpRequestStatus.PENDING);
    }
}
