package com.airh.relay.repository;

import com.airh.relay.domain.TaskLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskLogRepository extends JpaRepository<TaskLogEntity, String> {
    List<TaskLogEntity> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
