package com.wms.controller;

import com.wms.service.MesWmsFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mes-wms-flow")
@CrossOrigin(origins = "*")
public class MesWmsFlowController {
    
    @Autowired
    private MesWmsFlowService mesWmsFlowService;
    
    /**
     * 初始化MES-WMS交互流程
     * 4001设为1，4002设为1
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeFlow() {
        try {
            Map<String, Object> result = mesWmsFlowService.initializeMesWmsFlow();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "初始化流程失败: " + e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }
    
    /**
     * 检查并执行MES订单
     * 监控4007/4008，执行相应的出入库流程
     */
    @PostMapping("/check-and-execute")
    public ResponseEntity<Map<String, Object>> checkAndExecuteOrders() {
        try {
            Map<String, Object> result = mesWmsFlowService.checkAndExecuteOrders();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "检查执行订单失败: " + e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }
    
    /**
     * 获取MES-WMS流程状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFlowStatus() {
        try {
            Map<String, Object> status = mesWmsFlowService.getFlowStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("connected", false);
            errorResult.put("message", "获取流程状态失败: " + e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }
}


