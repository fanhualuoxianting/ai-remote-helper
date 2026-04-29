package com.airh.relay.repository;

import com.airh.relay.domain.FileChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileChangeRepository extends JpaRepository<FileChangeEntity, String> {
    List<FileChangeEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    List<FileChangeEntity> findByTaskId(String taskId);
}
