package com.airh.relay.repository;

import com.airh.relay.domain.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, String> {
    List<SessionEntity> findByStatus(String status);
    List<SessionEntity> findByDeviceId(String deviceId);
}
