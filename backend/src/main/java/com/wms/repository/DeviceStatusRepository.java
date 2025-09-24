package com.wms.repository;

import com.wms.entity.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceStatusRepository extends JpaRepository<DeviceStatus, Long> {
    
    DeviceStatus findFirstByOrderByIdAsc();
}
