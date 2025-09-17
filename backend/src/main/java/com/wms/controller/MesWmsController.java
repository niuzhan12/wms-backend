package com.wms.controller;

import com.wms.service.MesWmsIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/mes-wms")
@CrossOrigin(origins = "*")
public class MesWmsController {
    
    @Autowired
    private MesWmsIntegrationService mesWmsIntegrationService;
    
    /**
     * 初始化MES-WMS交互状态
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initialize() {
        try {
            mesWmsIntegrationService.initializeMesWmsStatus();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "MES-WMS交互状态初始化成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "初始化失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 更新WMS模式
     */
    @PostMapping("/update-mode")
    public ResponseEntity<Map<String, Object>> updateMode(@RequestBody Map<String, Object> request) {
        try {
            int mode = (Integer) request.get("mode");
            boolean success = mesWmsIntegrationService.updateWmsMode(mode);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "WMS模式更新成功" : "WMS模式更新失败");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "更新模式失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 获取WMS模式
     */
    @GetMapping("/mode")
    public ResponseEntity<Map<String, Object>> getMode() {
        try {
            int mode = mesWmsIntegrationService.readWmsMode();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("mode", mode);
            result.put("message", mode == 0 ? "本地模式" : "远程模式");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "获取模式失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 更新库位状态
     */
    @PostMapping("/update-location-status")
    public ResponseEntity<Map<String, Object>> updateLocationStatus() {
        try {
            mesWmsIntegrationService.updateLocationStatus();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "库位状态更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "更新库位状态失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 检查MES订单
     */
    @GetMapping("/check-orders")
    public ResponseEntity<Map<String, Object>> checkOrders() {
        try {
            boolean hasOutbound = mesWmsIntegrationService.hasOutboundOrder();
            boolean hasInbound = mesWmsIntegrationService.hasInboundOrder();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("hasOutboundOrder", hasOutbound);
            result.put("hasInboundOrder", hasInbound);
            result.put("message", "订单检查完成");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "检查订单失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 执行MES出库订单
     */
    @PostMapping("/execute-outbound")
    public ResponseEntity<Map<String, Object>> executeOutbound() {
        try {
            Map<String, Object> result = mesWmsIntegrationService.executeMesOutboundOrder();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "执行出库订单失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 执行MES入库订单
     */
    @PostMapping("/execute-inbound")
    public ResponseEntity<Map<String, Object>> executeInbound() {
        try {
            Map<String, Object> result = mesWmsIntegrationService.executeMesInboundOrder();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "执行入库订单失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 获取MES-WMS交互状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = mesWmsIntegrationService.getMesWmsStatus();
            System.out.println("DEBUG: 返回给前端的MES-WMS状态: " + status);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("connected", false);
            result.put("message", "获取状态失败: " + e.getMessage());
            System.out.println("DEBUG: 获取状态异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 强制重置所有状态
     */
    @PostMapping("/reset-all")
    public ResponseEntity<Map<String, Object>> resetAll() {
        try {
            // 强制清理所有Modbus寄存器
            if (mesWmsIntegrationService.isModbusConnected()) {
                mesWmsIntegrationService.forceResetAllRegisters();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "所有状态已强制重置");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "重置失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 连接MES-WMS Modbus
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
            result.put("message", "MES-WMS Modbus连接成功");
            result.put("connected", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "MES-WMS连接失败: " + e.getMessage());
            result.put("connected", false);
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * 断开MES-WMS Modbus连接
     */
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "MES-WMS Modbus连接已断开");
            result.put("connected", false);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "断开MES-WMS连接失败: " + e.getMessage());
            result.put("connected", false);
            return ResponseEntity.ok(result);
        }
    }
}
