package com.wms.repository;

import com.wms.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    
    /**
     * 根据模块查询日志
     */
    Page<OperationLog> findByModuleOrderByTimestampDesc(String module, Pageable pageable);
    
    /**
     * 根据级别查询日志
     */
    Page<OperationLog> findByLevelOrderByTimestampDesc(String level, Pageable pageable);
    
    /**
     * 根据时间范围查询日志
     */
    Page<OperationLog> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据模块和时间范围查询日志
     */
    Page<OperationLog> findByModuleAndTimestampBetweenOrderByTimestampDesc(
        String module, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 查询最近的日志
     */
    @Query("SELECT o FROM OperationLog o ORDER BY o.timestamp DESC")
    Page<OperationLog> findRecentLogs(Pageable pageable);
    
    /**
     * 根据关键词搜索日志
     */
    @Query("SELECT o FROM OperationLog o WHERE o.message LIKE %:keyword% OR o.operation LIKE %:keyword% ORDER BY o.timestamp DESC")
    Page<OperationLog> searchLogs(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 删除指定时间之前的日志
     */
    void deleteByTimestampBefore(LocalDateTime cutoffTime);
    
    /**
     * 统计日志数量
     */
    long countByModule(String module);
    
    /**
     * 统计指定时间范围内的日志数量
     */
    long countByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
}


