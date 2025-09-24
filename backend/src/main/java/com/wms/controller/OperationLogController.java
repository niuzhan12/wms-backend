package com.wms.controller;

import com.wms.entity.OperationLog;
import com.wms.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class OperationLogController {
    
    @Autowired
    private OperationLogService operationLogService;
    
    /**
     * 获取最近的日志
     */
    @GetMapping("/recent")
    public ResponseEntity<Page<OperationLog>> getRecentLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Page<OperationLog> logs = operationLogService.getRecentLogs(page, size);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取MES-WMS模块的日志
     */
    @GetMapping("/mes-wms")
    public ResponseEntity<Page<OperationLog>> getMesWmsLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Page<OperationLog> logs = operationLogService.getMesWmsLogs(page, size);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据模块获取日志
     */
    @GetMapping("/module/{module}")
    public ResponseEntity<Page<OperationLog>> getLogsByModule(
            @PathVariable String module,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Page<OperationLog> logs = operationLogService.getLogsByModule(module, page, size);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据级别获取日志
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<Page<OperationLog>> getLogsByLevel(
            @PathVariable String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Page<OperationLog> logs = operationLogService.getLogsByLevel(level, page, size);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 搜索日志
     */
    @GetMapping("/search")
    public ResponseEntity<Page<OperationLog>> searchLogs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Page<OperationLog> logs = operationLogService.searchLogs(keyword, page, size);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取日志统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getLogStatistics() {
        try {
            Map<String, Object> stats = operationLogService.getLogStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 清理旧日志
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldLogs(
            @RequestParam(defaultValue = "30") int daysToKeep) {
        try {
            operationLogService.cleanupOldLogs(daysToKeep);
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("success", true);
            result.put("message", "清理完成，保留最近" + daysToKeep + "天的日志");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("success", false);
            result.put("message", "清理失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}


