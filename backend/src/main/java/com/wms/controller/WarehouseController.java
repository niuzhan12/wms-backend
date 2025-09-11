package com.wms.controller;

import com.wms.service.WarehouseService;
import com.wms.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@CrossOrigin(origins = "*")
public class WarehouseController {
    
    @Autowired
    private WarehouseService warehouseService;
    
    @Autowired
    private DeviceService deviceService;
    
    // 获取仓库状态显示
    @GetMapping("/status/{warehouseCode}")
    public ResponseEntity<Map<String, Object>> getWarehouseStatus(@PathVariable String warehouseCode) {
        Map<String, Object> status = warehouseService.getWarehouseStatus(warehouseCode);
        return ResponseEntity.ok(status);
    }
    
    // 获取托盘分类
    @GetMapping("/pallets")
    public ResponseEntity<List<Map<String, Object>>> getPalletClassification() {
        List<Map<String, Object>> pallets = warehouseService.getPalletClassification();
        return ResponseEntity.ok(pallets);
    }
    
    // 获取货物分类
    @GetMapping("/goods")
    public ResponseEntity<List<Map<String, Object>>> getGoodsClassification() {
        List<Map<String, Object>> goods = warehouseService.getGoodsClassification();
        return ResponseEntity.ok(goods);
    }
    
    // 添加托盘
    @PostMapping("/pallets")
    public ResponseEntity<Map<String, Object>> addPallet(@RequestBody Map<String, Object> request) {
        boolean success = warehouseService.addPallet(request);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", success);
        response.put("message", success ? "托盘添加成功" : "托盘添加失败");
        return ResponseEntity.ok(response);
    }
    
    // 更新托盘
    @PutMapping("/pallets/{id}")
    public ResponseEntity<Map<String, Object>> updatePallet(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        boolean success = warehouseService.updatePallet(id, request);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", success);
        response.put("message", success ? "托盘更新成功" : "托盘更新失败");
        return ResponseEntity.ok(response);
    }
    
    // 删除托盘
    @DeleteMapping("/pallets/{id}")
    public ResponseEntity<Map<String, Object>> deletePallet(@PathVariable Long id) {
        boolean success = warehouseService.deletePallet(id);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", success);
        response.put("message", success ? "托盘删除成功" : "托盘删除失败");
        return ResponseEntity.ok(response);
    }
    
    // 添加货物
    @PostMapping("/goods")
    public ResponseEntity<Map<String, Object>> addGoods(@RequestBody Map<String, Object> request) {
        boolean success = warehouseService.addGoods(request);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", success);
        response.put("message", success ? "货物添加成功" : "货物添加失败");
        return ResponseEntity.ok(response);
    }
    
    // 更新货物
    @PutMapping("/goods/{id}")
    public ResponseEntity<Map<String, Object>> updateGoods(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        boolean success = warehouseService.updateGoods(id, request);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", success);
        response.put("message", success ? "货物更新成功" : "货物更新失败");
        return ResponseEntity.ok(response);
    }
    
    // 删除货物
    @DeleteMapping("/goods/{id}")
    public ResponseEntity<Map<String, Object>> deleteGoods(@PathVariable Long id) {
        boolean success = warehouseService.deleteGoods(id);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", success);
        response.put("message", success ? "货物删除成功" : "货物删除失败");
        return ResponseEntity.ok(response);
    }
    
    // 入库操作
    @PostMapping("/inbound")
    public ResponseEntity<Map<String, Object>> inbound(@RequestBody Map<String, Object> request) {
        String warehouseCode = (String) request.get("warehouseCode");
        Integer row = (Integer) request.get("row");
        Integer column = (Integer) request.get("column");
        String palletCode = (String) request.get("palletCode");
        String materialCode = (String) request.get("materialCode");
        
        boolean success = warehouseService.inbound(warehouseCode, row, column, palletCode, materialCode);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", success);
        response.put("message", success ? "入库成功" : "入库失败");
        
        return ResponseEntity.ok(response);
    }
    
    // 出库操作
    @PostMapping("/outbound")
    public ResponseEntity<Map<String, Object>> outbound(@RequestBody Map<String, Object> request) {
        String warehouseCode = (String) request.get("warehouseCode");
        Integer row = (Integer) request.get("row");
        Integer column = (Integer) request.get("column");
        
        boolean success = warehouseService.outbound(warehouseCode, row, column);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", success);
        response.put("message", success ? "出库成功" : "出库失败");
        
        return ResponseEntity.ok(response);
    }
    
    // 自动入库操作
    @PostMapping("/auto-inbound")
    public ResponseEntity<Map<String, Object>> autoInbound(@RequestBody Map<String, Object> request) {
        String warehouseCode = (String) request.get("warehouseCode");
        String palletCode = (String) request.get("palletCode");
        String materialCode = (String) request.get("materialCode");
        
        Map<String, Object> result = warehouseService.autoInbound(warehouseCode, palletCode, materialCode);
        return ResponseEntity.ok(result);
    }
    
    // 自动出库操作
    @PostMapping("/auto-outbound")
    public ResponseEntity<Map<String, Object>> autoOutbound(@RequestBody Map<String, Object> request) {
        String warehouseCode = (String) request.get("warehouseCode");
        
        Map<String, Object> result = warehouseService.autoOutbound(warehouseCode);
        return ResponseEntity.ok(result);
    }
    
    // 线体控制
    @PostMapping("/line-control")
    public ResponseEntity<Map<String, Object>> lineControl(@RequestBody Map<String, Object> request) {
        String operation = (String) request.get("operation");
        Integer row = (Integer) request.get("row");
        Integer column = (Integer) request.get("column");
        
        Map<String, Object> result = deviceService.lineControl(operation, row, column);
        return ResponseEntity.ok(result);
    }
    
}
