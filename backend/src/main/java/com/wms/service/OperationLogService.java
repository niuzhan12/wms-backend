package com.wms.service;

import com.wms.entity.OperationLog;
import com.wms.repository.OperationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Service
public class OperationLogService {
    
    @Autowired
    private OperationLogRepository operationLogRepository;
    
    /**
     * 保存操作日志
     */
    public void saveLog(String level, String message, String module, String operation) {
        try {
            OperationLog log = new OperationLog(level, message, module, operation);
            operationLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("保存操作日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存MES-WMS相关日志
     */
    public void saveMesWmsLog(String level, String message, String operation) {
        saveLog(level, message, "MES-WMS", operation);
    }
    
    /**
     * 获取最近的日志
     */
    public Page<OperationLog> getRecentLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return operationLogRepository.findRecentLogs(pageable);
    }
    
    /**
     * 根据模块获取日志
     */
    public Page<OperationLog> getLogsByModule(String module, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return operationLogRepository.findByModuleOrderByTimestampDesc(module, pageable);
    }
    
    /**
     * 根据级别获取日志
     */
    public Page<OperationLog> getLogsByLevel(String level, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return operationLogRepository.findByLevelOrderByTimestampDesc(level, pageable);
    }
    
    /**
     * 根据时间范围获取日志
     */
    public Page<OperationLog> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return operationLogRepository.findByTimestampBetweenOrderByTimestampDesc(startTime, endTime, pageable);
    }
    
    /**
     * 搜索日志
     */
    public Page<OperationLog> searchLogs(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return operationLogRepository.searchLogs(keyword, pageable);
    }
    
    /**
     * 获取MES-WMS模块的日志
     */
    public Page<OperationLog> getMesWmsLogs(int page, int size) {
        return getLogsByModule("MES-WMS", page, size);
    }
    
    /**
     * 清理旧日志
     */
    public void cleanupOldLogs(int daysToKeep) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
        operationLogRepository.deleteByTimestampBefore(cutoffTime);
    }
    
    /**
     * 获取日志统计信息
     */
    public Map<String, Object> getLogStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总日志数
        long totalLogs = operationLogRepository.count();
        stats.put("totalLogs", totalLogs);
        
        // MES-WMS模块日志数
        long mesWmsLogs = operationLogRepository.countByModule("MES-WMS");
        stats.put("mesWmsLogs", mesWmsLogs);
        
        // 今天的日志数
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        long todayLogs = operationLogRepository.countByTimestampBetween(todayStart, todayEnd);
        stats.put("todayLogs", todayLogs);
        
        // 最近7天的日志数
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long weekLogs = operationLogRepository.countByTimestampBetween(weekAgo, LocalDateTime.now());
        stats.put("weekLogs", weekLogs);
        
        return stats;
    }
}


