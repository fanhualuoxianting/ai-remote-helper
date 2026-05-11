package com.airh.relay.repository;

import com.airh.protocol.enums.TaskStatus;
import com.airh.relay.domain.TaskRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface TaskRecordRepository extends JpaRepository<TaskRecordEntity, String> {
    List<TaskRecordEntity> findByStatusInAndCreatedAtBefore(Collection<TaskStatus> statuses, Instant createdAtBefore);
}
