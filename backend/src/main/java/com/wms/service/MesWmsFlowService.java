package com.wms.service;

import com.wms.entity.WarehouseLocation;
import com.wms.repository.WarehouseLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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
    
    @Autowired
    private WmsStackerIntegrationService wmsStackerIntegrationService;
    
    // 防止重复处理的标记
    private volatile boolean isProcessing = false;
    
    /**
     * 清除处理状态（由堆垛机完成检查调用）
     */
    public void clearProcessingStatus() {
        isProcessing = false;
        System.out.println("DEBUG: WMS处理状态已清除");
    }
    
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
     * 2. 其他寄存器设为0 (WMS为空闲状态，等待订单)
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
            
            // 2. 初始化所有其他寄存器为0（包括WMS忙状态）
            modbusService.writeSingleRegister(WMS_BUSY, 0);  // WMS空闲
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
            result.put("wmsBusy", 0);  // WMS应该为空闲状态
            
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
        
        // 防止重复处理
        if (isProcessing) {
            result.put("success", true);
            result.put("message", "正在处理中，跳过本次检查");
            result.put("hasOrder", false);
            return result;
        }
        
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
            
            System.out.println("DEBUG: 检查MES订单 - 出库订单:" + outboundOrder + ", 入库订单:" + inboundOrder);
            
            if (outboundOrder == 1) {
                // 检查是否已经在处理中
                if (isProcessing) {
                    result.put("success", true);
                    result.put("message", "出库流程正在处理中，等待堆垛机完成");
                    result.put("hasOrder", true);
                    return result;
                }
                
                // 执行出库流程
                System.out.println("DEBUG: 检测到出库订单，开始执行出库流程");
                operationLogService.saveMesWmsLog("INFO", "检测到MES出库订单，开始执行出库流程", "订单检测");
                isProcessing = true;
                try {
                    Map<String, Object> flowResult = executeOutboundFlow();
                    if ((Boolean) flowResult.get("success")) {
                        // 出库指令已发送给堆垛机，保持处理状态，等待堆垛机完成
                        result.put("success", true);
                        result.put("message", "出库指令已发送给堆垛机，等待堆垛机完成");
                        result.put("hasOrder", true);
                        return result;
                    } else {
                        // 出库流程失败，清除处理状态
                        isProcessing = false;
                        return flowResult;
                    }
                } catch (Exception e) {
                    isProcessing = false;
                    result.put("success", false);
                    result.put("message", "执行出库流程失败: " + e.getMessage());
                    return result;
                }
            } else if (inboundOrder == 1) {
                // 检查是否已经在处理中
                if (isProcessing) {
                    result.put("success", true);
                    result.put("message", "入库流程正在处理中，等待堆垛机完成");
                    result.put("hasOrder", true);
                    return result;
                }
                
                // 执行入库流程
                System.out.println("DEBUG: 检测到入库订单，开始执行入库流程");
                operationLogService.saveMesWmsLog("INFO", "检测到MES入库订单，开始执行入库流程", "订单检测");
                isProcessing = true;
                try {
                    Map<String, Object> flowResult = executeInboundFlow();
                    if ((Boolean) flowResult.get("success")) {
                        // 入库指令已发送给堆垛机，保持处理状态，等待堆垛机完成
                        result.put("success", true);
                        result.put("message", "入库指令已发送给堆垛机，等待堆垛机完成");
                        result.put("hasOrder", true);
                        return result;
                    } else {
                        // 入库流程失败，清除处理状态
                        isProcessing = false;
                        return flowResult;
                    }
                } catch (Exception e) {
                    isProcessing = false;
                    result.put("success", false);
                    result.put("message", "执行入库流程失败: " + e.getMessage());
                    return result;
                }
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
        
        System.out.println("DEBUG: 步骤1 - WMS接收MES出库信号，设置WMS为忙状态");
        operationLogService.saveMesWmsLog("INFO", "步骤1: WMS接收MES出库信号，设置WMS为忙状态", "出库流程");
        
        try {
            // 1. WMS接收MES信号 → WMS状态变为"忙"
            modbusService.writeSingleRegister(WMS_BUSY, 1);
            // 2. 设置WMS出库中状态
            modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 1);
            operationLogService.saveMesWmsLog("INFO", "WMS状态已设置为忙碌，出库进度已启动", "出库流程");
            
            System.out.println("DEBUG: 步骤2 - WMS查找有料的库位");
            operationLogService.saveMesWmsLog("INFO", "步骤2: WMS开始查找有料的库位", "出库流程");
            // 3. 执行出库操作
            List<WarehouseLocation> locations = locationRepository.findByWarehouseCodeOrderByRowNumberAscColumnNumberAsc("A");
            
            for (WarehouseLocation location : locations) {
                if (location.getHasPallet()) {
                    System.out.println("DEBUG: 步骤3 - 找到目标位置: " + location.getRowNumber() + "行" + location.getColumnNumber() + "列");
                    operationLogService.saveMesWmsLog("INFO", 
                        String.format("步骤3: 找到目标位置 %d行%d列", location.getRowNumber(), location.getColumnNumber()), 
                        "出库流程");
                    // 4. 设置当前执行的行列
                    modbusService.writeSingleRegister(WMS_CURRENT_ROW, location.getRowNumber());
                    modbusService.writeSingleRegister(WMS_CURRENT_COLUMN, location.getColumnNumber());
                    operationLogService.saveMesWmsLog("INFO", 
                        String.format("已设置当前位置为 %d行%d列", location.getRowNumber(), location.getColumnNumber()), 
                        "出库流程");
                    
                    System.out.println("DEBUG: 步骤4 - WMS下发指令给堆垛机");
                    operationLogService.saveMesWmsLog("INFO", "步骤4: WMS开始下发指令给堆垛机", "出库流程");
                    
                    // 5. 给堆垛机发送出库指令（不等待完成）
                    Map<String, Object> stackerResult = wmsStackerIntegrationService.sendOutboundCommand(
                        location.getRowNumber(), location.getColumnNumber());
                    
                    if (!(Boolean) stackerResult.get("success")) {
                        operationLogService.saveMesWmsLog("ERROR", 
                            "下发出库指令到堆垛机失败: " + stackerResult.get("message"), 
                            "出库流程");
                        result.put("success", false);
                        result.put("message", "下发出库指令到堆垛机失败: " + stackerResult.get("message"));
                        return result;
                    }
                    
                    System.out.println("DEBUG: 步骤5 - 出库指令已下发到堆垛机，等待堆垛机完成");
                    operationLogService.saveMesWmsLog("INFO", 
                        String.format("步骤5: 出库指令已下发到堆垛机，目标位置 %d行%d列，等待堆垛机完成", 
                                    location.getRowNumber(), location.getColumnNumber()), 
                        "出库流程");
                    
                    result.put("success", true);
                    result.put("message", "出库指令已下发到堆垛机，等待堆垛机完成");
                    result.put("row", location.getRowNumber());
                    result.put("column", location.getColumnNumber());
                    result.put("operation", "outbound");
                    
                    System.out.println("DEBUG: 出库指令已下发到堆垛机，位置: " + location.getRowNumber() + "行" + location.getColumnNumber() + "列");
                    return result;
                }
            }
            
            // 没有找到有料的库位
            modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_BUSY, 0);
            // 清除MES出库订单 (4007 = 0) - WMS主动清除
            modbusService.writeSingleRegister(MES_OUTBOUND_ORDER, 0);
            
            operationLogService.saveMesWmsLog("WARNING", "出库流程执行失败 - 没有找到有料的库位", "出库流程");
            
            result.put("success", false);
            result.put("message", "没有找到有料的库位");
            
        } catch (Exception e) {
            // 出错时清理状态
            modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_BUSY, 0);
            // 清除MES出库订单 (4007 = 0) - WMS主动清除
            modbusService.writeSingleRegister(MES_OUTBOUND_ORDER, 0);
            
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
            // 1. 设置WMS忙碌状态 (4002 = 1)
            modbusService.writeSingleRegister(WMS_BUSY, 1);
            // 2. 设置WMS入库中状态 (4004 = 1)
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
                    
                    // 通过堆垛机执行入库操作（WMS只下发指令，不直接操作库位）
                    Map<String, Object> stackerResult = wmsStackerIntegrationService.sendInboundCommand(
                        location.getRowNumber(), location.getColumnNumber());
                    
                    if (!(Boolean) stackerResult.get("success")) {
                        result.put("success", false);
                        result.put("message", "下发入库指令到堆垛机失败: " + stackerResult.get("message"));
                        return result;
                    }
                    
                    // WMS不直接操作库位，等待堆垛机完成后再更新
                    System.out.println("DEBUG: 入库指令已下发到堆垛机，等待堆垛机完成");
                    
                    result.put("success", true);
                    result.put("message", "入库指令已下发到堆垛机，等待堆垛机完成");
                    result.put("row", location.getRowNumber());
                    result.put("column", location.getColumnNumber());
                    result.put("operation", "inbound");
                    
                    System.out.println("DEBUG: 入库指令已下发到堆垛机，位置: " + location.getRowNumber() + "行" + location.getColumnNumber() + "列");
                    return result;
                }
            }
            
            // 没有找到空闲的库位
            modbusService.writeSingleRegister(WMS_INBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_BUSY, 0);
            // 清除MES入库订单 (4008 = 0) - WMS主动清除
            modbusService.writeSingleRegister(MES_INBOUND_ORDER, 0);
            
            result.put("success", false);
            result.put("message", "没有找到空闲的库位");
            
        } catch (Exception e) {
            // 出错时清理状态
            modbusService.writeSingleRegister(WMS_INBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_BUSY, 0);
            // 清除MES入库订单 (4008 = 0) - WMS主动清除
            modbusService.writeSingleRegister(MES_INBOUND_ORDER, 0);
            
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
    
    /**
     * 定期检查MES订单并执行
     * 每2秒检查一次
     */
    @Scheduled(fixedRate = 2000)
    public void checkMesOrders() {
        try {
            System.out.println("DEBUG: 定时任务 - 检查MES订单");
            checkAndExecuteOrders();
        } catch (Exception e) {
            System.err.println("检查MES订单失败: " + e.getMessage());
        }
    }
    
    /**
     * 定期检查堆垛机完成状态
     * 每2秒检查一次
     */
    @Scheduled(fixedRate = 2000)
    public void checkStackerCompletion() {
        try {
            System.out.println("DEBUG: 定时任务 - 检查堆垛机完成状态");
            wmsStackerIntegrationService.checkStackerCompletion();
        } catch (Exception e) {
            System.err.println("检查堆垛机完成状态失败: " + e.getMessage());
        }
    }
}
