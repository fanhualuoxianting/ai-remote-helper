package com.airh.relay.repository;

import com.airh.relay.domain.TaskRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRecordRepository extends JpaRepository<TaskRecordEntity, String> {
}
