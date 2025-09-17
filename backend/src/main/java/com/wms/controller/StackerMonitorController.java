package com.wms.controller;

import com.wms.service.WmsStackerIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stacker-monitor")
@CrossOrigin(origins = "*")
public class StackerMonitorController {
    
    @Autowired
    private WmsStackerIntegrationService wmsStackerIntegrationService;
    
    /**
     * 获取WMS-堆垛机集成状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = wmsStackerIntegrationService.getWmsStackerStatus();
            System.out.println("DEBUG: 返回给前端的WMS-堆垛机状态: " + status);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            System.err.println("获取WMS-堆垛机状态异常: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 初始化WMS-堆垛机状态
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initialize() {
        try {
            wmsStackerIntegrationService.initializeWmsStackerStatus();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "WMS-堆垛机状态初始化成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("初始化WMS-堆垛机状态异常: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 执行出库操作
     */
    @PostMapping("/outbound")
    public ResponseEntity<Map<String, Object>> executeOutbound(@RequestParam int row, @RequestParam int column) {
        try {
            Map<String, Object> result = wmsStackerIntegrationService.executeOutboundFlow(row, column);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("执行出库操作异常: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 执行入库操作
     */
    @PostMapping("/inbound")
    public ResponseEntity<Map<String, Object>> executeInbound(@RequestParam int row, @RequestParam int column) {
        try {
            Map<String, Object> result = wmsStackerIntegrationService.executeInboundFlow(row, column);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("执行入库操作异常: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 强制重置所有状态
     */
    @PostMapping("/reset-all")
    public ResponseEntity<Map<String, Object>> resetAll() {
        try {
            wmsStackerIntegrationService.forceResetAllStatus();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "所有状态已强制重置");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("强制重置状态异常: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 检查堆垛机完成状态
     */
    @PostMapping("/check-completion")
    public ResponseEntity<Map<String, Object>> checkCompletion() {
        try {
            wmsStackerIntegrationService.checkStackerCompletion();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "完成状态检查完成");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("检查完成状态异常: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 连接WMS-堆垛机 Modbus
     */
    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> connect(@RequestBody Map<String, Object> request) {
        try {
            String host = (String) request.get("host");
            Integer port = (Integer) request.get("port");
            Integer slaveId = (Integer) request.get("slaveId");
            
            // 这里可以添加连接逻辑，目前直接返回成功
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "WMS-堆垛机 Modbus连接成功");
            result.put("connected", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "WMS-堆垛机连接失败: " + e.getMessage());
            result.put("connected", false);
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 断开WMS-堆垛机 Modbus连接
     */
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "WMS-堆垛机 Modbus连接已断开");
            result.put("connected", false);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "断开WMS-堆垛机连接失败: " + e.getMessage());
            result.put("connected", false);
            return ResponseEntity.ok(result);
        }
    }
}
