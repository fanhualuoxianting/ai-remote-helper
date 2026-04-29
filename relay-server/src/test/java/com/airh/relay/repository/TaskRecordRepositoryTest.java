package com.airh.relay.repository;

import com.airh.protocol.enums.TaskStatus;
import com.airh.protocol.enums.TaskType;
import com.airh.relay.domain.TaskRecordEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRecordRepositoryTest {
    @Autowired
    private TaskRecordRepository taskRecordRepository;

    @Test
    void savesAndUpdatesTaskRecord() {
        String taskId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        TaskRecordEntity entity = new TaskRecordEntity(
                taskId,
                "session-1",
                TaskType.LIST_DIR,
                TaskStatus.PENDING,
                "{\"data\":{\"path\":\".\"}}",
                null,
                null,
                null,
                null,
                now,
                now,
                null
        );

        taskRecordRepository.save(entity);
        Optional<TaskRecordEntity> saved = taskRecordRepository.findById(taskId);

        assertThat(saved).isPresent();
        assertThat(saved.get().getTaskType()).isEqualTo(TaskType.LIST_DIR);
        assertThat(saved.get().getStatus()).isEqualTo(TaskStatus.PENDING);

        saved.get().complete(TaskStatus.SUCCESS, "完成", "[]", "", null, Instant.now());
        taskRecordRepository.save(saved.get());

        TaskRecordEntity updated = taskRecordRepository.findById(taskId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.SUCCESS);
        assertThat(updated.getSummary()).isEqualTo("完成");
        assertThat(updated.getCompletedAt()).isNotNull();
    }
}
