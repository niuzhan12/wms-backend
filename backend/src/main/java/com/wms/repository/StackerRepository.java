package com.wms.repository;

import com.wms.entity.Stacker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StackerRepository extends JpaRepository<Stacker, Long> {
    
    /**
     * 根据IP地址查找堆垛机
     */
    Optional<Stacker> findByIpAddress(String ipAddress);
    
    /**
     * 根据名称查找堆垛机
     */
    Optional<Stacker> findByName(String name);
}
