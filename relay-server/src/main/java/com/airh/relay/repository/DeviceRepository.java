package com.airh.relay.repository;

import com.airh.relay.domain.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, String> {
    List<DeviceEntity> findByStatus(String status);
}
