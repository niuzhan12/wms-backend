package com.wms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WmsStackerIntegrationService {
    
    @Autowired
    private StackerModbusService stackerModbusService;
    
    
    @Autowired
    private MesWmsIntegrationService mesWmsIntegrationService;
    
    @Autowired
    private ModbusService modbusService;
    
    @Autowired
    private com.wms.repository.WarehouseLocationRepository locationRepository;
    
    // 操作类型常量
    private static final int OPERATION_INBOUND = 1;
    private static final int OPERATION_OUTBOUND = 2;
    
    /**
     * 获取WMS-堆垛机集成状态
     */
    public Map<String, Object> getWmsStackerStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查Modbus连接
            boolean connected = stackerModbusService.isConnected();
            result.put("connected", connected);
            
            if (!connected) {
                result.put("error", "堆垛机Modbus连接未建立");
                return result;
            }
            
            // 读取堆垛机状态
            int stackerStatus = stackerModbusService.getStackerStatus();
            int stackerOperation = stackerModbusService.getStackerOperation();
            int[] position = stackerModbusService.getStackerPosition();
            int progress = stackerModbusService.getStackerProgress();
            int complete = stackerModbusService.getStackerComplete();
            
            result.put("stackerStatus", stackerStatus);
            result.put("stackerOperation", stackerOperation);
            result.put("stackerCurrentRow", position[0]);
            result.put("stackerCurrentColumn", position[1]);
            result.put("stackerProgress", progress);
            result.put("stackerComplete", complete);
            
            // 获取MES-WMS状态
            Map<String, Object> mesWmsStatus = mesWmsIntegrationService.getMesWmsStatus();
            result.putAll(mesWmsStatus);
            
            // 计算整体状态
            boolean wmsBusy = (Integer) mesWmsStatus.get("wmsBusy") == 1;
            boolean stackerBusy = stackerStatus == 1;
            boolean hasOrder = (Integer) mesWmsStatus.get("mesOutboundOrder") == 1 || 
                              (Integer) mesWmsStatus.get("mesInboundOrder") == 1;
            
            result.put("systemBusy", wmsBusy || stackerBusy);
            result.put("hasOrder", hasOrder);
            
            System.out.println("DEBUG: WMS-堆垛机状态 - 连接:" + connected + 
                             ", 堆垛机状态:" + stackerStatus + 
                             ", 操作:" + stackerOperation + 
                             ", 位置:" + position[0] + "行" + position[1] + "列" +
                             ", 进度:" + progress + "%" +
                             ", 完成:" + complete);
            
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", "获取状态失败: " + e.getMessage());
            System.err.println("获取WMS-堆垛机状态失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 初始化WMS-堆垛机状态
     */
    public void initializeWmsStackerStatus() {
        try {
            // 初始化堆垛机寄存器
            if (stackerModbusService.isConnected()) {
                System.out.println("DEBUG: 初始化堆垛机Modbus寄存器...");
                try {
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_MODE, 1); // 堆垛机的模式 (远程模式)
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_STATUS, 0); // 堆垛机是否在忙
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_OUTBOUND_PROGRESS, 0); // 堆垛机出库中
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_INBOUND_PROGRESS, 0); // 堆垛机入库中
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_OUTBOUND_COMPLETE, 0); // 堆垛机出库完成
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_INBOUND_COMPLETE, 0); // 堆垛机入库完成
                    stackerModbusService.writeSingleRegister(StackerModbusService.WMS_OUTBOUND_ORDER, 0); // WMS下单出库
                    stackerModbusService.writeSingleRegister(StackerModbusService.WMS_INBOUND_ORDER, 0); // WMS下单入库
                    stackerModbusService.writeSingleRegister(StackerModbusService.WMS_ORDER_ROW, 0); // WMS下单行
                    stackerModbusService.writeSingleRegister(StackerModbusService.WMS_ORDER_COLUMN, 0); // WMS下单列
                    System.out.println("DEBUG: 堆垛机Modbus寄存器初始化完成");
                } catch (Exception e) {
                    System.err.println("DEBUG: 初始化堆垛机Modbus寄存器失败: " + e.getMessage());
                }
            } else {
                System.out.println("DEBUG: 堆垛机Modbus连接未建立，跳过初始化");
            }
            
            // 初始化MES-WMS状态
            mesWmsIntegrationService.initializeMesWmsStatus();
            
            System.out.println("WMS-堆垛机集成状态初始化完成");
            
        } catch (Exception e) {
            System.err.println("初始化WMS-堆垛机状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行出库流程（WMS -> 堆垛机）
     */
    public Map<String, Object> executeOutboundFlow(int targetRow, int targetColumn) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            System.out.println("DEBUG: 开始执行出库流程 - 目标位置: " + targetRow + "行" + targetColumn + "列");
            
            // 1. 设置WMS忙碌状态 (4002 = 1)
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_BUSY, 1);
            // 2. 设置WMS出库中状态 (4003 = 1)
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_OUTBOUND_PROGRESS, 1);
            // 3. 设置当前执行的行列 (4009/4010)
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_CURRENT_ROW, targetRow);
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_CURRENT_COLUMN, targetColumn);
            
            // 4. 发送出库指令到堆垛机
            stackerModbusService.sendOperationCommand(OPERATION_OUTBOUND, targetRow, targetColumn);
            
            result.put("success", true);
            result.put("message", "出库指令已发送到堆垛机");
            result.put("targetRow", targetRow);
            result.put("targetColumn", targetColumn);
            
            System.out.println("DEBUG: 出库指令已发送到堆垛机");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "执行出库流程失败: " + e.getMessage());
            System.err.println("执行出库流程失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 执行入库流程（WMS -> 堆垛机）
     */
    public Map<String, Object> executeInboundFlow(int targetRow, int targetColumn) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            System.out.println("DEBUG: 开始执行入库流程 - 目标位置: " + targetRow + "行" + targetColumn + "列");
            
            // 1. 设置WMS忙碌状态 (4002 = 1)
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_BUSY, 1);
            // 2. 设置WMS入库中状态 (4004 = 1)
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_INBOUND_PROGRESS, 1);
            // 3. 设置当前执行的行列 (4009/4010)
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_CURRENT_ROW, targetRow);
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_CURRENT_COLUMN, targetColumn);
            
            // 4. 发送入库指令到堆垛机
            stackerModbusService.sendOperationCommand(OPERATION_INBOUND, targetRow, targetColumn);
            
            result.put("success", true);
            result.put("message", "入库指令已发送到堆垛机");
            result.put("targetRow", targetRow);
            result.put("targetColumn", targetColumn);
            
            System.out.println("DEBUG: 入库指令已发送到堆垛机");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "执行入库流程失败: " + e.getMessage());
            System.err.println("执行入库流程失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 检查堆垛机完成状态并更新WMS
     */
    public void checkStackerCompletion() {
        try {
            int stackerComplete = stackerModbusService.getStackerComplete();
            int stackerOperation = stackerModbusService.getStackerOperation();
            
            if (stackerComplete == 1) {
                System.out.println("DEBUG: 检测到堆垛机操作完成");
                
                // 读取WMS记录的目标位置
                int row = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_CURRENT_ROW, 1)[0];
                int col = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_CURRENT_COLUMN, 1)[0];
                
                if (stackerOperation == OPERATION_OUTBOUND) {
                    // 出库完成 -> 更新库位为空
                    locationRepository.findByWarehouseCodeOrderByRowNumberAscColumnNumberAsc("A")
                        .stream()
                        .filter(l -> l.getRowNumber() == row && l.getColumnNumber() == col)
                        .findFirst()
                        .ifPresent(l -> {
                            l.setHasPallet(false);
                            l.setPalletCode(null);
                            l.setMaterialCode(null);
                            l.setMaterialStatus(null);
                            locationRepository.save(l);
                        });
                    
                    // 出库完成寄存器
                    modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_OUTBOUND_PROGRESS, 0);
                    modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_OUTBOUND_COMPLETE, 1);
                    modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_BUSY, 0);
                    
                    // 清除MES出库订单
                    modbusService.writeSingleRegister(MesWmsIntegrationService.MES_OUTBOUND_ORDER, 0);
                    
                    System.out.println("DEBUG: 出库流程完成，已通知MES");
                    
                } else if (stackerOperation == OPERATION_INBOUND) {
                    // 入库完成 -> 更新库位为有托盘
                    locationRepository.findByWarehouseCodeOrderByRowNumberAscColumnNumberAsc("A")
                        .stream()
                        .filter(l -> l.getRowNumber() == row && l.getColumnNumber() == col)
                        .findFirst()
                        .ifPresent(l -> {
                            l.setHasPallet(true);
                            l.setPalletCode("AUTO-" + System.currentTimeMillis());
                            l.setMaterialCode("MATERIAL-" + System.currentTimeMillis());
                            l.setMaterialStatus(com.wms.entity.WarehouseLocation.MaterialStatus.RAW);
                            locationRepository.save(l);
                        });
                    
                    // 入库完成寄存器
                    modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_INBOUND_PROGRESS, 0);
                    modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_INBOUND_COMPLETE, 1);
                    modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_BUSY, 0);
                    
                    // 清除MES入库订单
                    modbusService.writeSingleRegister(MesWmsIntegrationService.MES_INBOUND_ORDER, 0);
                    
                    System.out.println("DEBUG: 入库流程完成，已通知MES");
                }
                
                // 重置堆垛机状态
                stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_STATUS, 0); // 堆垛机是否在忙
                stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_OUTBOUND_PROGRESS, 0); // 堆垛机出库中
                stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_INBOUND_PROGRESS, 0); // 堆垛机入库中
                stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_OUTBOUND_COMPLETE, 0); // 堆垛机出库完成
                stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_INBOUND_COMPLETE, 0); // 堆垛机入库完成
                stackerModbusService.writeSingleRegister(StackerModbusService.WMS_OUTBOUND_ORDER, 0); // WMS下单出库
                stackerModbusService.writeSingleRegister(StackerModbusService.WMS_INBOUND_ORDER, 0); // WMS下单入库
                stackerModbusService.writeSingleRegister(StackerModbusService.WMS_ORDER_ROW, 0); // WMS下单行
                stackerModbusService.writeSingleRegister(StackerModbusService.WMS_ORDER_COLUMN, 0); // WMS下单列
                
                // 延迟后清除完成标志
                Thread.sleep(2000);
                modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_OUTBOUND_COMPLETE, 0);
                modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_INBOUND_COMPLETE, 0);
                modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_CURRENT_ROW, 0);
                modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_CURRENT_COLUMN, 0);
                
            }
            
        } catch (Exception e) {
            System.err.println("检查堆垛机完成状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 强制重置所有状态
     */
    public void forceResetAllStatus() {
        try {
            System.out.println("DEBUG: 开始强制重置WMS-堆垛机所有状态...");
            
            // 重置堆垛机状态 (端口503)
            if (stackerModbusService.isConnected()) {
                System.out.println("DEBUG: 重置堆垛机Modbus寄存器 (端口503)...");
                try {
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_MODE, 1); // 堆垛机的模式 (远程模式)
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_STATUS, 0); // 堆垛机是否在忙
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_OUTBOUND_PROGRESS, 0); // 堆垛机出库中
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_INBOUND_PROGRESS, 0); // 堆垛机入库中
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_OUTBOUND_COMPLETE, 0); // 堆垛机出库完成
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_INBOUND_COMPLETE, 0); // 堆垛机入库完成
                    stackerModbusService.writeSingleRegister(StackerModbusService.WMS_OUTBOUND_ORDER, 0); // WMS下单出库
                    stackerModbusService.writeSingleRegister(StackerModbusService.WMS_INBOUND_ORDER, 0); // WMS下单入库
                    stackerModbusService.writeSingleRegister(StackerModbusService.WMS_ORDER_ROW, 0); // WMS下单行
                    stackerModbusService.writeSingleRegister(StackerModbusService.WMS_ORDER_COLUMN, 0); // WMS下单列
                    System.out.println("DEBUG: 堆垛机Modbus寄存器重置完成");
                } catch (Exception e) {
                    System.err.println("DEBUG: 重置堆垛机Modbus寄存器失败: " + e.getMessage());
                }
            } else {
                System.out.println("DEBUG: 堆垛机Modbus连接未建立，跳过重置");
            }
            
            // 重置MES-WMS状态 (端口502)
            if (mesWmsIntegrationService.isModbusConnected()) {
                System.out.println("DEBUG: 重置MES-WMS Modbus寄存器 (端口502)...");
                mesWmsIntegrationService.forceResetAllRegisters();
                System.out.println("DEBUG: MES-WMS Modbus寄存器重置完成");
            } else {
                System.out.println("DEBUG: MES-WMS Modbus连接未建立，跳过重置");
            }
            
            System.out.println("DEBUG: WMS-堆垛机所有状态已强制重置");
            
        } catch (Exception e) {
            System.err.println("强制重置状态失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
