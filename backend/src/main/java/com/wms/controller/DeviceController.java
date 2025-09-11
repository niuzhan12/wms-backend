package com.wms.controller;

import com.wms.entity.DeviceStatus;
import com.wms.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/device")
@CrossOrigin(origins = "*")
public class DeviceController {
    
    @Autowired
    private DeviceService deviceService;
    
    // 获取设备状态
    @GetMapping("/status")
    public ResponseEntity<DeviceStatus> getDeviceStatus() {
        DeviceStatus status = deviceService.getDeviceStatus();
        return ResponseEntity.ok(status);
    }
    
    // 更新设备状态
    @PutMapping("/status")
    public ResponseEntity<DeviceStatus> updateDeviceStatus(@RequestBody DeviceStatus deviceStatus) {
        DeviceStatus updatedStatus = deviceService.updateDeviceStatus(deviceStatus);
        return ResponseEntity.ok(updatedStatus);
    }
    
    // 设置设备空闲
    @PostMapping("/idle")
    public ResponseEntity<Map<String, Object>> setDeviceIdle() {
        deviceService.setDeviceIdle();
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("message", "设备已设置为空闲状态");
        return ResponseEntity.ok(response);
    }
    
    // 设置设备忙
    @PostMapping("/busy")
    public ResponseEntity<Map<String, Object>> setDeviceBusy(@RequestBody Map<String, Object> request) {
        Integer row = (Integer) request.get("row");
        Integer column = (Integer) request.get("column");
        
        deviceService.setDeviceBusy(row, column);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("message", "设备已设置为忙状态");
        return ResponseEntity.ok(response);
    }
    
    // 切换控制模式
    @PostMapping("/toggle-control-mode")
    public ResponseEntity<Map<String, Object>> toggleControlMode() {
        deviceService.toggleControlMode();
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("message", "控制模式已切换");
        return ResponseEntity.ok(response);
    }
    
    // 出入库复位
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        deviceService.setDeviceIdle();
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("message", "出入库复位成功");
        return ResponseEntity.ok(response);
    }
}
