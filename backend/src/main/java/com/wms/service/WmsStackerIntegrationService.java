package com.wms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    @Lazy
    private MesWmsFlowService mesWmsFlowService;
    
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
            
            // 确保状态同步
            result.put("systemBusy", wmsBusy || stackerBusy);
            result.put("hasOrder", hasOrder);
            
            // 添加调试信息
            System.out.println("DEBUG: 状态同步检查 - WMS忙:" + wmsBusy + ", 堆垛机忙:" + stackerBusy + ", 有订单:" + hasOrder);
            
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
     * 发送出库指令给堆垛机（不等待完成）
     */
    public Map<String, Object> sendOutboundCommand(int targetRow, int targetColumn) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            System.out.println("DEBUG: 发送出库指令给堆垛机 - 目标位置: " + targetRow + "行" + targetColumn + "列");
            
            // 检查堆垛机连接
            if (!stackerModbusService.isConnected()) {
                result.put("success", false);
                result.put("message", "堆垛机Modbus连接未建立");
                return result;
            }
            
            // 发送出库指令到堆垛机
            stackerModbusService.sendOperationCommand(OPERATION_OUTBOUND, targetRow, targetColumn);
            
            // 记录操作日志
            operationLogService.saveMesWmsLog("INFO", 
                String.format("WMS发送出库指令给堆垛机，目标位置: %d行%d列", targetRow, targetColumn), 
                "WMS-堆垛机通信");
            
            result.put("success", true);
            result.put("message", "出库指令已发送到堆垛机");
            result.put("targetRow", targetRow);
            result.put("targetColumn", targetColumn);
            
            System.out.println("DEBUG: 出库指令已发送到堆垛机");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送出库指令失败: " + e.getMessage());
            System.err.println("发送出库指令失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 发送入库指令给堆垛机（不等待完成）
     */
    public Map<String, Object> sendInboundCommand(int targetRow, int targetColumn) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            System.out.println("DEBUG: 发送入库指令给堆垛机 - 目标位置: " + targetRow + "行" + targetColumn + "列");
            
            // 检查堆垛机连接
            if (!stackerModbusService.isConnected()) {
                result.put("success", false);
                result.put("message", "堆垛机Modbus连接未建立");
                return result;
            }
            
            // 发送入库指令到堆垛机
            stackerModbusService.sendOperationCommand(OPERATION_INBOUND, targetRow, targetColumn);
            
            // 记录操作日志
            operationLogService.saveMesWmsLog("INFO", 
                String.format("WMS发送入库指令给堆垛机，目标位置: %d行%d列", targetRow, targetColumn), 
                "WMS-堆垛机通信");
            
            result.put("success", true);
            result.put("message", "入库指令已发送到堆垛机");
            result.put("targetRow", targetRow);
            result.put("targetColumn", targetColumn);
            
            System.out.println("DEBUG: 入库指令已发送到堆垛机");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送入库指令失败: " + e.getMessage());
            System.err.println("发送入库指令失败: " + e.getMessage());
        }
        
        return result;
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
            
            // 5. 同时设置MES-WMS的入库订单状态
            modbusService.writeSingleRegister(MesWmsIntegrationService.MES_INBOUND_ORDER, 1);
            
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
     * 检查超时的操作并自动完成
     */
    private void checkTimeoutOperations() {
        try {
            // 检查是否有进行中的操作
            int inboundProgress = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_INBOUND_PROGRESS, 1)[0];
            int outboundProgress = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_OUTBOUND_PROGRESS, 1)[0];
            
            // 暂时禁用自动完成，让正常的堆垛机完成流程处理
            // TODO: 添加真正的时间戳检查逻辑
            System.out.println("DEBUG: 检查超时操作 - inboundProgress:" + inboundProgress + ", outboundProgress:" + outboundProgress);
            
            // 注释掉自动完成逻辑，让正常的堆垛机完成流程处理
            /*
            if (inboundProgress == 1) {
                // 检查入库操作是否超时（假设30秒超时）
                System.out.println("DEBUG: 检测到入库操作进行中，检查是否超时");
                // 这里可以添加时间戳检查逻辑
                // 如果超时，自动完成操作
                autoCompleteInboundOperation();
            } else if (outboundProgress == 1) {
                // 检查出库操作是否超时
                System.out.println("DEBUG: 检测到出库操作进行中，检查是否超时");
                autoCompleteOutboundOperation();
            }
            */
        } catch (Exception e) {
            System.err.println("检查超时操作失败: " + e.getMessage());
        }
    }
    
    /**
     * 自动完成入库操作
     */
    private void autoCompleteInboundOperation() {
        try {
            System.out.println("DEBUG: 自动完成入库操作");
            
            // 读取目标位置
            int row = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_CURRENT_ROW, 1)[0];
            int col = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_CURRENT_COLUMN, 1)[0];
            
            // 更新库位状态
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
            
            // 更新寄存器状态
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_INBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_INBOUND_COMPLETE, 1);
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_BUSY, 0);
            modbusService.writeSingleRegister(MesWmsIntegrationService.MES_INBOUND_ORDER, 0);
            
            // 清除堆垛机状态
            stackerModbusService.stopStackerOperation();
            
            System.out.println("DEBUG: 入库操作已自动完成");
        } catch (Exception e) {
            System.err.println("自动完成入库操作失败: " + e.getMessage());
        }
    }
    
    /**
     * 自动完成出库操作
     */
    private void autoCompleteOutboundOperation() {
        try {
            System.out.println("DEBUG: 自动完成出库操作");
            
            // 读取目标位置
            int row = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_CURRENT_ROW, 1)[0];
            int col = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_CURRENT_COLUMN, 1)[0];
            
            // 更新库位状态
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
            
            // 更新寄存器状态
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_OUTBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_OUTBOUND_COMPLETE, 1);
            modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_BUSY, 0);
            modbusService.writeSingleRegister(MesWmsIntegrationService.MES_OUTBOUND_ORDER, 0);
            
            // 清除堆垛机状态
            stackerModbusService.stopStackerOperation();
            
            System.out.println("DEBUG: 出库操作已自动完成");
        } catch (Exception e) {
            System.err.println("自动完成出库操作失败: " + e.getMessage());
        }
    }
    
    /**
     * 在操作过程中更新当前位置信息
     */
    private void updateCurrentPositionDuringOperation() {
        try {
            // 检查是否有正在进行的操作
            int wmsBusy = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_BUSY, 1)[0];
            int wmsOutboundProgress = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_OUTBOUND_PROGRESS, 1)[0];
            int wmsInboundProgress = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_INBOUND_PROGRESS, 1)[0];
            
            // 如果有正在进行的操作，确保当前位置信息正确显示
            if (wmsBusy == 1 || wmsOutboundProgress == 1 || wmsInboundProgress == 1) {
                // 读取当前设置的位置
                int currentRow = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_CURRENT_ROW, 1)[0];
                int currentColumn = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_CURRENT_COLUMN, 1)[0];
                
                // 如果当前位置为0，说明可能被意外清除了，尝试从堆垛机状态恢复
                if (currentRow == 0 && currentColumn == 0) {
                    int[] stackerPosition = stackerModbusService.getStackerPosition();
                    if (stackerPosition[0] > 0 && stackerPosition[1] > 0) {
                        modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_CURRENT_ROW, stackerPosition[0]);
                        modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_CURRENT_COLUMN, stackerPosition[1]);
                        System.out.println("DEBUG: 从堆垛机状态恢复当前位置: " + stackerPosition[0] + "行" + stackerPosition[1] + "列");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("更新操作过程中当前位置失败: " + e.getMessage());
        }
    }
    
    // 记录操作开始时间
    private long operationStartTime = 0;
    private int currentOperation = 0;
    
    /**
     * 模拟堆垛机完成操作
     */
    private void simulateStackerCompletion(int operation) {
        try {
            long currentTime = System.currentTimeMillis();
            
            // 如果是新操作，记录开始时间
            if (operationStartTime == 0 || currentOperation != operation) {
                operationStartTime = currentTime;
                currentOperation = operation;
                System.out.println("DEBUG: 开始模拟堆垛机操作 - 类型:" + operation + ", 时间:" + currentTime);
                operationLogService.saveMesWmsLog("INFO", 
                    "堆垛机开始执行" + (operation == OPERATION_OUTBOUND ? "出库" : "入库") + "操作", 
                    operation == OPERATION_OUTBOUND ? "出库流程" : "入库流程");
                return;
            }
            
            // 检查是否已经操作了5秒
            if (currentTime - operationStartTime >= 5000) {
                // 设置堆垛机完成标志
                if (operation == OPERATION_OUTBOUND) {
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_OUTBOUND_COMPLETE, 1);
                    System.out.println("DEBUG: 模拟堆垛机出库完成");
                    operationLogService.saveMesWmsLog("SUCCESS", "堆垛机出库操作完成", "出库流程");
                } else if (operation == OPERATION_INBOUND) {
                    stackerModbusService.writeSingleRegister(StackerModbusService.STACKER_INBOUND_COMPLETE, 1);
                    System.out.println("DEBUG: 模拟堆垛机入库完成");
                    operationLogService.saveMesWmsLog("SUCCESS", "堆垛机入库操作完成", "入库流程");
                }
                
                // 重置操作状态
                operationStartTime = 0;
                currentOperation = 0;
                
                // 注意：不要在这里重置堆垛机状态，让checkStackerCompletion来处理
            }
        } catch (Exception e) {
            System.err.println("模拟堆垛机完成失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查堆垛机完成状态并更新WMS
     */
    public void checkStackerCompletion() {
        // 首先检查是否有超时的操作
        checkTimeoutOperations();
        
        // 在操作过程中更新当前位置信息
        updateCurrentPositionDuringOperation();
        
        try {
            // 检查是否有正在进行的操作
            int stackerStatus = stackerModbusService.getStackerStatus();
            int stackerOperation = stackerModbusService.getStackerOperation();
            int wmsOutboundProgress = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_OUTBOUND_PROGRESS, 1)[0];
            int wmsInboundProgress = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_INBOUND_PROGRESS, 1)[0];
            
            // 如果有正在进行的操作，模拟堆垛机完成（5秒后自动完成）
            System.out.println("DEBUG: 检查堆垛机状态 - stackerStatus:" + stackerStatus + ", stackerOperation:" + stackerOperation);
            if (stackerStatus == 1) {
                System.out.println("DEBUG: 检测到堆垛机正在操作，模拟完成检查");
                
                // 模拟堆垛机完成操作
                simulateStackerCompletion(stackerOperation);
            } else {
                System.out.println("DEBUG: 堆垛机状态为空闲，跳过模拟");
            }
            
            int stackerComplete = stackerModbusService.getStackerComplete();
            
            if (stackerComplete == 1) {
                System.out.println("DEBUG: 检测到堆垛机操作完成");
                
                // 读取WMS记录的目标位置
                int row = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_CURRENT_ROW, 1)[0];
                int col = modbusService.readHoldingRegisters(MesWmsIntegrationService.WMS_CURRENT_COLUMN, 1)[0];
                
                if (stackerOperation == OPERATION_OUTBOUND) {
                    System.out.println("DEBUG: 步骤6 - 堆垛机出库完成，通知WMS");
                    operationLogService.saveMesWmsLog("INFO", 
                        String.format("步骤6: 堆垛机出库完成，位置 %d行%d列，通知WMS", row, col), 
                        "出库流程");
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
                    
                    System.out.println("DEBUG: 步骤7 - WMS接收堆垛机完成信号，清除WMS状态");
                    operationLogService.saveMesWmsLog("INFO", "步骤7: WMS接收堆垛机完成信号，清除WMS状态", "出库流程");
                    // WMS接收堆垛机完成信号，清除WMS状态
                    modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_OUTBOUND_PROGRESS, 0);
                    modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_OUTBOUND_COMPLETE, 1);
                    modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_BUSY, 0);
                    
                    System.out.println("DEBUG: 步骤8 - WMS通知MES出库完成，清除MES订单");
                    operationLogService.saveMesWmsLog("INFO", "步骤8: WMS通知MES出库完成，清除MES订单", "出库流程");
                    // WMS通知MES出库完成，清除MES订单
                    modbusService.writeSingleRegister(MesWmsIntegrationService.MES_OUTBOUND_ORDER, 0);
                    
                    System.out.println("DEBUG: 出库流程完成，已通知MES");
                    operationLogService.saveMesWmsLog("SUCCESS", "出库流程完成，已通知MES，所有状态已清除", "出库流程");
                    
                    // 清除WMS处理状态
                    mesWmsFlowService.clearProcessingStatus();
                    
                } else if (stackerOperation == OPERATION_INBOUND) {
                    System.out.println("DEBUG: 步骤6 - 堆垛机入库完成，通知WMS");
                    operationLogService.saveMesWmsLog("INFO", 
                        String.format("步骤6: 堆垛机入库完成，位置 %d行%d列，通知WMS", row, col), 
                        "入库流程");
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
                    
                    System.out.println("DEBUG: 步骤7 - WMS接收堆垛机完成信号，清除WMS状态");
                    operationLogService.saveMesWmsLog("INFO", "步骤7: WMS接收堆垛机完成信号，清除WMS状态", "入库流程");
                    // WMS接收堆垛机完成信号，清除WMS状态
                    modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_INBOUND_PROGRESS, 0);
                    modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_INBOUND_COMPLETE, 1);
                    modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_BUSY, 0);
                    
                    System.out.println("DEBUG: 步骤8 - WMS通知MES入库完成，清除MES订单");
                    operationLogService.saveMesWmsLog("INFO", "步骤8: WMS通知MES入库完成，清除MES订单", "入库流程");
                    // WMS通知MES入库完成，清除MES订单
                    modbusService.writeSingleRegister(MesWmsIntegrationService.MES_INBOUND_ORDER, 0);
                    
                    System.out.println("DEBUG: 入库流程完成，已通知MES");
                    operationLogService.saveMesWmsLog("SUCCESS", "入库流程完成，已通知MES，所有状态已清除", "入库流程");
                    
                    // 清除WMS处理状态
                    mesWmsFlowService.clearProcessingStatus();
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
                
                // 清除当前位置信息
                modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_CURRENT_ROW, 0);
                modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_CURRENT_COLUMN, 0);
                
                // 延迟后清除完成标志
                Thread.sleep(2000);
                modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_OUTBOUND_COMPLETE, 0);
                modbusService.writeSingleRegister(MesWmsIntegrationService.WMS_INBOUND_COMPLETE, 0);
                
                System.out.println("DEBUG: 所有状态已重置，操作完成");
                
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
