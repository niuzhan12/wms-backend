package com.wms.service;

import com.wms.entity.WarehouseLocation;
import com.wms.repository.WarehouseLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MesWmsFlowService {
    
    @Autowired
    private ModbusService modbusService;
    
    @Autowired
    private WarehouseLocationRepository locationRepository;
    
    @Autowired
    private OperationLogService operationLogService;
    
    // MES-WMS交互寄存器地址定义
    private static final int WMS_MODE = 4001;           // WMS模式 (0:本地, 1:远程)
    private static final int WMS_BUSY = 4002;           // WMS是否在忙 (0:空闲, 1:忙)
    private static final int WMS_OUTBOUND_PROGRESS = 4003;  // WMS出库中 (0:空闲, 1:出库中)
    private static final int WMS_INBOUND_PROGRESS = 4004;   // WMS入库中 (0:空闲, 1:入库中)
    private static final int WMS_OUTBOUND_COMPLETE = 4005;  // WMS出库完成 (0:空闲, 1:完成)
    private static final int WMS_INBOUND_COMPLETE = 4006;   // WMS入库完成 (0:空闲, 1:完成)
    private static final int MES_OUTBOUND_ORDER = 4007;     // MES下单出库 (0:空闲, 1:出库)
    private static final int MES_INBOUND_ORDER = 4008;      // MES下单入库 (0:空闲, 1:入库)
    private static final int WMS_CURRENT_ROW = 4009;        // WMS当前执行的行
    private static final int WMS_CURRENT_COLUMN = 4010;     // WMS当前执行的列
    
    /**
     * 初始化MES-WMS交互流程
     * 1. 4001设为1 (WMS设置为远程状态)
     * 2. 4002设为1 (WMS改为忙状态)
     */
    public Map<String, Object> initializeMesWmsFlow() {
        Map<String, Object> result = new HashMap<>();
        
        if (!modbusService.isConnected()) {
            result.put("success", false);
            result.put("message", "Modbus连接未建立");
            return result;
        }
        
        try {
            // 1. 设置WMS模式为远程 (4001 = 1)
            boolean modeSet = modbusService.writeSingleRegister(WMS_MODE, 1);
            if (!modeSet) {
                result.put("success", false);
                result.put("message", "设置WMS模式失败");
                return result;
            }
            
            // 2. 设置WMS为忙状态 (4002 = 1)
            boolean busySet = modbusService.writeSingleRegister(WMS_BUSY, 1);
            if (!busySet) {
                result.put("success", false);
                result.put("message", "设置WMS忙状态失败");
                return result;
            }
            
            // 初始化其他寄存器为0
            modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_INBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_OUTBOUND_COMPLETE, 0);
            modbusService.writeSingleRegister(WMS_INBOUND_COMPLETE, 0);
            modbusService.writeSingleRegister(MES_OUTBOUND_ORDER, 0);
            modbusService.writeSingleRegister(MES_INBOUND_ORDER, 0);
            modbusService.writeSingleRegister(WMS_CURRENT_ROW, 0);
            modbusService.writeSingleRegister(WMS_CURRENT_COLUMN, 0);
            
            // 记录日志
            operationLogService.saveMesWmsLog("SUCCESS", "MES-WMS交互流程初始化成功", "初始化流程");
            
            result.put("success", true);
            result.put("message", "MES-WMS交互流程初始化成功");
            result.put("wmsMode", 1);
            result.put("wmsBusy", 1);
            
        } catch (Exception e) {
            operationLogService.saveMesWmsLog("ERROR", "初始化MES-WMS流程失败: " + e.getMessage(), "初始化流程");
            result.put("success", false);
            result.put("message", "初始化MES-WMS流程失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 检查MES订单并执行相应的出入库操作
     * 监控4007/4008寄存器变化，执行相应的出入库流程
     */
    public Map<String, Object> checkAndExecuteOrders() {
        Map<String, Object> result = new HashMap<>();
        
        if (!modbusService.isConnected()) {
            result.put("success", false);
            result.put("message", "Modbus连接未建立");
            return result;
        }
        
        try {
            // 检查MES订单
            int[] orderValues = modbusService.readHoldingRegisters(MES_OUTBOUND_ORDER, 2);
            if (orderValues == null || orderValues.length < 2) {
                result.put("success", false);
                result.put("message", "读取MES订单状态失败");
                return result;
            }
            
            int outboundOrder = orderValues[0]; // 4007
            int inboundOrder = orderValues[1];  // 4008
            
            if (outboundOrder == 1) {
                // 执行出库流程
                return executeOutboundFlow();
            } else if (inboundOrder == 1) {
                // 执行入库流程
                return executeInboundFlow();
            } else {
                result.put("success", true);
                result.put("message", "无MES订单");
                result.put("hasOrder", false);
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "检查MES订单失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 执行出库流程
     * 1. 4003设为1 (WMS出库中)
     * 2. 执行出库操作
     * 3. 4009/4010设置实际行列
     * 4. 4005设为1 (出库完成)
     * 5. 4007设为0 (清除MES出库订单)
     * 6. 4003设为0 (出库中结束)
     * 7. 4005设为0 (出库完成结束)
     * 8. 4002设为0 (WMS空闲)
     */
    private Map<String, Object> executeOutboundFlow() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 设置WMS出库中状态 (4003 = 1)
            modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 1);
            
            // 2. 执行出库操作
            List<WarehouseLocation> locations = locationRepository.findByWarehouseCodeOrderByRowNumberAscColumnNumberAsc("A");
            
            for (WarehouseLocation location : locations) {
                if (location.getHasPallet()) {
                    // 3. 设置当前执行的行列 (4009/4010)
                    modbusService.writeSingleRegister(WMS_CURRENT_ROW, location.getRowNumber());
                    modbusService.writeSingleRegister(WMS_CURRENT_COLUMN, location.getColumnNumber());
                    
                    // 记录开始执行日志
                    operationLogService.saveMesWmsLog("INFO", 
                        String.format("开始执行出库 - 位置: %d行%d列", location.getRowNumber(), location.getColumnNumber()), 
                        "出库流程");
                    
                    // 执行出库
                    location.setHasPallet(false);
                    location.setPalletCode(null);
                    location.setMaterialCode(null);
                    location.setMaterialStatus(null);
                    locationRepository.save(location);
                    
                    // 4. 设置出库完成状态 (4005 = 1)
                    modbusService.writeSingleRegister(WMS_OUTBOUND_COMPLETE, 1);
                    
                    // 注意：4007由MES负责清除，WMS不主动清除
                    
                    // 6. 结束出库中状态 (4003 = 0)
                    modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 0);
                    
                    // 7. 结束出库完成状态 (4005 = 0)
                    modbusService.writeSingleRegister(WMS_OUTBOUND_COMPLETE, 0);
                    
                    // 8. 设置WMS空闲 (4002 = 0)
                    modbusService.writeSingleRegister(WMS_BUSY, 0);
                    
                    // 延迟清除当前执行位置，让用户能看到执行结果
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000); // 等待3秒
                            modbusService.writeSingleRegister(WMS_CURRENT_ROW, 0);
                            modbusService.writeSingleRegister(WMS_CURRENT_COLUMN, 0);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                    
                    // 记录出库成功日志
                    operationLogService.saveMesWmsLog("SUCCESS", 
                        String.format("出库流程执行成功 - 位置: %d行%d列", location.getRowNumber(), location.getColumnNumber()), 
                        "出库流程");
                    
                    result.put("success", true);
                    result.put("message", "出库流程执行成功");
                    result.put("row", location.getRowNumber());
                    result.put("column", location.getColumnNumber());
                    result.put("operation", "outbound");
                    return result;
                }
            }
            
            // 没有找到有料的库位
            modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_BUSY, 0);
            // 注意：4007由MES负责清除，WMS不主动清除
            
            operationLogService.saveMesWmsLog("WARNING", "出库流程执行失败 - 没有找到有料的库位", "出库流程");
            
            result.put("success", false);
            result.put("message", "没有找到有料的库位");
            
        } catch (Exception e) {
            // 出错时清理状态
            modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_BUSY, 0);
            // 注意：4007由MES负责清除，WMS不主动清除
            
            result.put("success", false);
            result.put("message", "执行出库流程失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 执行入库流程
     * 1. 4004设为1 (WMS入库中)
     * 2. 执行入库操作
     * 3. 4009/4010设置实际行列
     * 4. 4006设为1 (入库完成)
     * 5. 4008设为0 (清除MES入库订单)
     * 6. 4004设为0 (入库中结束)
     * 7. 4006设为0 (入库完成结束)
     * 8. 4002设为0 (WMS空闲)
     */
    private Map<String, Object> executeInboundFlow() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 设置WMS入库中状态 (4004 = 1)
            modbusService.writeSingleRegister(WMS_INBOUND_PROGRESS, 1);
            
            // 2. 执行入库操作
            List<WarehouseLocation> locations = locationRepository.findByWarehouseCodeOrderByRowNumberAscColumnNumberAsc("A");
            
            for (WarehouseLocation location : locations) {
                if (!location.getHasPallet()) {
                    // 3. 设置当前执行的行列 (4009/4010)
                    modbusService.writeSingleRegister(WMS_CURRENT_ROW, location.getRowNumber());
                    modbusService.writeSingleRegister(WMS_CURRENT_COLUMN, location.getColumnNumber());
                    
                    // 记录开始执行日志
                    operationLogService.saveMesWmsLog("INFO", 
                        String.format("开始执行入库 - 位置: %d行%d列", location.getRowNumber(), location.getColumnNumber()), 
                        "入库流程");
                    
                    // 执行入库（简化操作，不填具体物品和托盘信息）
                    location.setHasPallet(true);
                    location.setPalletCode("AUTO-" + System.currentTimeMillis());
                    location.setMaterialCode("MATERIAL-" + System.currentTimeMillis());
                    location.setMaterialStatus(WarehouseLocation.MaterialStatus.RAW);
                    locationRepository.save(location);
                    
                    // 4. 设置入库完成状态 (4006 = 1)
                    modbusService.writeSingleRegister(WMS_INBOUND_COMPLETE, 1);
                    
                    // 注意：4008由MES负责清除，WMS不主动清除
                    
                    // 6. 结束入库中状态 (4004 = 0)
                    modbusService.writeSingleRegister(WMS_INBOUND_PROGRESS, 0);
                    
                    // 7. 结束入库完成状态 (4006 = 0)
                    modbusService.writeSingleRegister(WMS_INBOUND_COMPLETE, 0);
                    
                    // 8. 设置WMS空闲 (4002 = 0)
                    modbusService.writeSingleRegister(WMS_BUSY, 0);
                    
                    // 延迟清除当前执行位置，让用户能看到执行结果
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000); // 等待3秒
                            modbusService.writeSingleRegister(WMS_CURRENT_ROW, 0);
                            modbusService.writeSingleRegister(WMS_CURRENT_COLUMN, 0);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                    
                    // 记录入库成功日志
                    operationLogService.saveMesWmsLog("SUCCESS", 
                        String.format("入库流程执行成功 - 位置: %d行%d列", location.getRowNumber(), location.getColumnNumber()), 
                        "入库流程");
                    
                    result.put("success", true);
                    result.put("message", "入库流程执行成功");
                    result.put("row", location.getRowNumber());
                    result.put("column", location.getColumnNumber());
                    result.put("operation", "inbound");
                    return result;
                }
            }
            
            // 没有找到空闲的库位
            modbusService.writeSingleRegister(WMS_INBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_BUSY, 0);
            // 注意：4008由MES负责清除，WMS不主动清除
            
            result.put("success", false);
            result.put("message", "没有找到空闲的库位");
            
        } catch (Exception e) {
            // 出错时清理状态
            modbusService.writeSingleRegister(WMS_INBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_BUSY, 0);
            // 注意：4008由MES负责清除，WMS不主动清除
            
            result.put("success", false);
            result.put("message", "执行入库流程失败: " + e.getMessage());
        }
        
        return result;
    }
    
    
    /**
     * 获取当前MES-WMS流程状态
     */
    public Map<String, Object> getFlowStatus() {
        Map<String, Object> status = new HashMap<>();
        
        if (!modbusService.isConnected()) {
            status.put("connected", false);
            status.put("message", "Modbus连接未建立");
            return status;
        }
        
        try {
            // 读取所有状态寄存器
            int[] values = modbusService.readHoldingRegisters(4001, 10);
            
            if (values != null && values.length >= 10) {
                status.put("connected", true);
                status.put("wmsMode", values[0]); // 4001
                status.put("wmsBusy", values[1]); // 4002
                status.put("wmsOutboundProgress", values[2]); // 4003
                status.put("wmsInboundProgress", values[3]); // 4004
                status.put("wmsOutboundComplete", values[4]); // 4005
                status.put("wmsInboundComplete", values[5]); // 4006
                status.put("mesOutboundOrder", values[6]); // 4007
                status.put("mesInboundOrder", values[7]); // 4008
                status.put("wmsCurrentRow", values[8]); // 4009
                status.put("wmsCurrentColumn", values[9]); // 4010
            } else {
                status.put("connected", false);
                status.put("message", "读取状态寄存器失败");
            }
            
        } catch (Exception e) {
            status.put("connected", false);
            status.put("message", "获取流程状态失败: " + e.getMessage());
        }
        
        return status;
    }
}
