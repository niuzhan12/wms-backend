package com.wms.controller;

import com.wms.service.ModbusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/modbus")
@CrossOrigin(origins = "*")
public class ModbusController {
    
    @Autowired
    private ModbusService modbusService;
    
    /**
     * 检查Modbus连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("connected", modbusService.isConnected());
        response.put("message", modbusService.isConnected() ? "Modbus连接正常" : "Modbus连接断开");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 读取保持寄存器
     */
    @GetMapping("/read/{startAddress}/{quantity}")
    public ResponseEntity<Map<String, Object>> readRegisters(
            @PathVariable int startAddress, 
            @PathVariable int quantity) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (!modbusService.isConnected()) {
            response.put("success", false);
            response.put("message", "Modbus连接未建立");
            return ResponseEntity.ok(response);
        }
        
        int[] values = modbusService.readHoldingRegisters(startAddress, quantity);
        
        if (values != null) {
            response.put("success", true);
            response.put("data", values);
            response.put("message", "读取成功");
        } else {
            response.put("success", false);
            response.put("message", "读取失败");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 写入单个寄存器
     */
    @PostMapping("/write/{address}")
    public ResponseEntity<Map<String, Object>> writeRegister(
            @PathVariable int address, 
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (!modbusService.isConnected()) {
            response.put("success", false);
            response.put("message", "Modbus连接未建立");
            return ResponseEntity.ok(response);
        }
        
        Integer value = (Integer) request.get("value");
        if (value == null) {
            response.put("success", false);
            response.put("message", "值不能为空");
            return ResponseEntity.ok(response);
        }
        
        boolean success = modbusService.writeSingleRegister(address, value);
        
        response.put("success", success);
        response.put("message", success ? "写入成功" : "写入失败");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 建立连接
     */
    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> connect(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        if (modbusService.isConnected()) {
            response.put("success", true);
            response.put("message", "连接已存在");
        } else {
            String host = (String) request.getOrDefault("host", "127.0.0.1");
            Integer port = (Integer) request.getOrDefault("port", 502);
            Integer slaveId = (Integer) request.getOrDefault("slaveId", 1);
            
            modbusService.connect(host, port, slaveId);
            response.put("success", modbusService.isConnected());
            response.put("message", modbusService.isConnected() ? 
                "连接成功: " + host + ":" + port + " (从站ID:" + slaveId + ")" : 
                "连接失败: " + host + ":" + port);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 断开连接
     */
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect() {
        Map<String, Object> response = new HashMap<>();
        
        if (!modbusService.isConnected()) {
            response.put("success", true);
            response.put("message", "连接已断开");
        } else {
            modbusService.closeConnection();
            response.put("success", true);
            response.put("message", "连接已断开");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 重新连接
     */
    @PostMapping("/reconnect")
    public ResponseEntity<Map<String, Object>> reconnect() {
        Map<String, Object> response = new HashMap<>();
        
        modbusService.reconnect();
        
        response.put("success", modbusService.isConnected());
        response.put("message", modbusService.isConnected() ? "重连成功" : "重连失败");
        
        return ResponseEntity.ok(response);
    }
}
