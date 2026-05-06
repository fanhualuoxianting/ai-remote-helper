package com.airh.relay.repository;

import com.airh.relay.domain.HelpRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HelpRequestRepository extends JpaRepository<HelpRequestEntity, String> {
    List<HelpRequestEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId);
}
