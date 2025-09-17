package com.wms.service;

import com.wms.entity.WarehouseLocation;
import com.wms.repository.WarehouseLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MesWmsIntegrationService {
    
    @Autowired
    private ModbusService modbusService;
    
    @Autowired
    private WarehouseLocationRepository locationRepository;
    
    // MES-WMS交互寄存器地址定义
    public static final int WMS_MODE = 4001;           // WMS模式 (0:本地, 1:远程)
    public static final int WMS_BUSY = 4002;           // WMS是否在忙 (0:空闲, 1:忙)
    public static final int WMS_OUTBOUND_PROGRESS = 4003;  // WMS出库中 (0:空闲, 1:出库中)
    public static final int WMS_INBOUND_PROGRESS = 4004;   // WMS入库中 (0:空闲, 1:入库中)
    public static final int WMS_OUTBOUND_COMPLETE = 4005;  // WMS出库完成 (0:空闲, 1:完成)
    public static final int WMS_INBOUND_COMPLETE = 4006;   // WMS入库完成 (0:空闲, 1:完成)
    public static final int MES_OUTBOUND_ORDER = 4007;     // MES下单出库 (0:空闲, 1:出库)
    public static final int MES_INBOUND_ORDER = 4008;      // MES下单入库 (0:空闲, 1:入库)
    public static final int WMS_CURRENT_ROW = 4009;        // WMS当前执行的行
    public static final int WMS_CURRENT_COLUMN = 4010;     // WMS当前执行的列
    public static final int LOCATION_STATUS_START = 4011;  // 库位状态起始地址 (4011-4035)
    
    /**
     * 初始化MES-WMS交互状态
     */
    public void initializeMesWmsStatus() {
        if (!modbusService.isConnected()) {
            System.err.println("Modbus连接未建立，无法初始化MES-WMS状态");
            return;
        }
        
        try {
            // 初始化所有寄存器为0
            for (int i = 4001; i <= 4035; i++) {
                modbusService.writeSingleRegister(i, 0);
            }
            
            // 设置WMS模式为本地模式
            modbusService.writeSingleRegister(WMS_MODE, 0);
            
            // 更新库位状态
            updateLocationStatus();
            
            System.out.println("MES-WMS交互状态初始化完成");
        } catch (Exception e) {
            System.err.println("初始化MES-WMS状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新WMS模式到MES
     */
    public boolean updateWmsMode(int mode) {
        if (!modbusService.isConnected()) {
            return false;
        }
        
        try {
            boolean success = modbusService.writeSingleRegister(WMS_MODE, mode);
            if (success) {
                System.out.println("WMS模式已更新到MES: " + (mode == 0 ? "本地模式" : "远程模式"));
            }
            return success;
        } catch (Exception e) {
            System.err.println("更新WMS模式失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 从MES读取WMS模式
     */
    public int readWmsMode() {
        if (!modbusService.isConnected()) {
            return 0; // 默认本地模式
        }
        
        try {
            int[] values = modbusService.readHoldingRegisters(WMS_MODE, 1);
            return values != null && values.length > 0 ? values[0] : 0;
        } catch (Exception e) {
            System.err.println("读取WMS模式失败: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 更新库位状态到MES (4011-4035)
     */
    public void updateLocationStatus() {
        if (!modbusService.isConnected()) {
            return;
        }
        
        try {
            // 获取A库的所有库位
            List<WarehouseLocation> locations = locationRepository.findByWarehouseCodeOrderByRowNumberAscColumnNumberAsc("A");
            
            // 更新库位状态寄存器 (4011-4035)
            for (int i = 0; i < 25; i++) {
                int registerAddress = LOCATION_STATUS_START + i;
                int hasPallet = 0;
                
                if (i < locations.size()) {
                    WarehouseLocation location = locations.get(i);
                    hasPallet = location.getHasPallet() ? 1 : 0;
                }
                
                modbusService.writeSingleRegister(registerAddress, hasPallet);
            }
            
            System.out.println("库位状态已更新到MES");
        } catch (Exception e) {
            System.err.println("更新库位状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查MES是否有出库订单
     */
    public boolean hasOutboundOrder() {
        if (!modbusService.isConnected()) {
            return false;
        }
        
        try {
            int[] values = modbusService.readHoldingRegisters(MES_OUTBOUND_ORDER, 1);
            return values != null && values.length > 0 && values[0] == 1;
        } catch (Exception e) {
            System.err.println("检查出库订单失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查MES是否有入库订单
     */
    public boolean hasInboundOrder() {
        if (!modbusService.isConnected()) {
            return false;
        }
        
        try {
            int[] values = modbusService.readHoldingRegisters(MES_INBOUND_ORDER, 1);
            return values != null && values.length > 0 && values[0] == 1;
        } catch (Exception e) {
            System.err.println("检查入库订单失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行MES出库订单
     */
    public Map<String, Object> executeMesOutboundOrder() {
        Map<String, Object> result = new HashMap<>();
        
        if (!modbusService.isConnected()) {
            result.put("success", false);
            result.put("message", "Modbus连接未建立");
            return result;
        }
        
        try {
            // 设置WMS出库中状态
            modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 1);
            modbusService.writeSingleRegister(WMS_BUSY, 1);
            
            // 查找第一个有料的库位进行出库
            List<WarehouseLocation> locations = locationRepository.findByWarehouseCodeOrderByRowNumberAscColumnNumberAsc("A");
            
            for (WarehouseLocation location : locations) {
                if (location.getHasPallet()) {
                    // 设置当前执行的行和列
                    modbusService.writeSingleRegister(WMS_CURRENT_ROW, location.getRowNumber());
                    modbusService.writeSingleRegister(WMS_CURRENT_COLUMN, location.getColumnNumber());
                    
                    // 执行出库
                    location.setHasPallet(false);
                    location.setPalletCode(null);
                    location.setMaterialCode(null);
                    location.setMaterialStatus(null);
                    locationRepository.save(location);
                    
                    // 设置出库完成状态
                    modbusService.writeSingleRegister(WMS_OUTBOUND_COMPLETE, 1);
                    modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 0);
                    modbusService.writeSingleRegister(WMS_BUSY, 0);
                    modbusService.writeSingleRegister(WMS_CURRENT_ROW, 0);
                    modbusService.writeSingleRegister(WMS_CURRENT_COLUMN, 0);
                    
                    // 清除MES出库订单
                    modbusService.writeSingleRegister(MES_OUTBOUND_ORDER, 0);
                    
                    // 更新库位状态
                    updateLocationStatus();
                    
                    result.put("success", true);
                    result.put("message", "MES出库订单执行成功");
                    result.put("row", location.getRowNumber());
                    result.put("column", location.getColumnNumber());
                    return result;
                }
            }
            
            // 没有找到有料的库位
            modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_BUSY, 0);
            
            result.put("success", false);
            result.put("message", "没有找到有料的库位");
            return result;
            
        } catch (Exception e) {
            System.err.println("执行MES出库订单失败: " + e.getMessage());
            modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_BUSY, 0);
            
            result.put("success", false);
            result.put("message", "执行MES出库订单失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 执行MES入库订单
     */
    public Map<String, Object> executeMesInboundOrder() {
        Map<String, Object> result = new HashMap<>();
        
        if (!modbusService.isConnected()) {
            result.put("success", false);
            result.put("message", "Modbus连接未建立");
            return result;
        }
        
        try {
            // 设置WMS入库中状态
            modbusService.writeSingleRegister(WMS_INBOUND_PROGRESS, 1);
            modbusService.writeSingleRegister(WMS_BUSY, 1);
            
            // 查找第一个空闲的库位进行入库
            List<WarehouseLocation> locations = locationRepository.findByWarehouseCodeOrderByRowNumberAscColumnNumberAsc("A");
            
            for (WarehouseLocation location : locations) {
                if (!location.getHasPallet()) {
                    // 设置当前执行的行和列
                    modbusService.writeSingleRegister(WMS_CURRENT_ROW, location.getRowNumber());
                    modbusService.writeSingleRegister(WMS_CURRENT_COLUMN, location.getColumnNumber());
                    
                    // 执行入库（简化操作，不填具体物品和托盘信息）
                    location.setHasPallet(true);
                    location.setPalletCode("AUTO-" + System.currentTimeMillis());
                    location.setMaterialCode("MATERIAL-" + System.currentTimeMillis());
                    location.setMaterialStatus(WarehouseLocation.MaterialStatus.RAW);
                    locationRepository.save(location);
                    
                    // 设置入库完成状态
                    modbusService.writeSingleRegister(WMS_INBOUND_COMPLETE, 1);
                    modbusService.writeSingleRegister(WMS_INBOUND_PROGRESS, 0);
                    modbusService.writeSingleRegister(WMS_BUSY, 0);
                    modbusService.writeSingleRegister(WMS_CURRENT_ROW, 0);
                    modbusService.writeSingleRegister(WMS_CURRENT_COLUMN, 0);
                    
                    // 清除MES入库订单
                    modbusService.writeSingleRegister(MES_INBOUND_ORDER, 0);
                    
                    // 更新库位状态
                    updateLocationStatus();
                    
                    result.put("success", true);
                    result.put("message", "MES入库订单执行成功");
                    result.put("row", location.getRowNumber());
                    result.put("column", location.getColumnNumber());
                    return result;
                }
            }
            
            // 没有找到空闲的库位
            modbusService.writeSingleRegister(WMS_INBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_BUSY, 0);
            
            result.put("success", false);
            result.put("message", "没有找到空闲的库位");
            return result;
            
        } catch (Exception e) {
            System.err.println("执行MES入库订单失败: " + e.getMessage());
            modbusService.writeSingleRegister(WMS_INBOUND_PROGRESS, 0);
            modbusService.writeSingleRegister(WMS_BUSY, 0);
            
            result.put("success", false);
            result.put("message", "执行MES入库订单失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 获取MES-WMS交互状态
     */
    public Map<String, Object> getMesWmsStatus() {
        Map<String, Object> status = new HashMap<>();
        
        if (!modbusService.isConnected()) {
            status.put("connected", false);
            status.put("message", "Modbus连接未建立");
            System.out.println("DEBUG: Modbus连接未建立，使用默认值");
        } else {
            status.put("connected", true);
            System.out.println("DEBUG: Modbus连接正常");
        }
        
        try {
            // 从Modbus读取MES-WMS交互状态寄存器
            int[] values = null;
            if (modbusService.isConnected()) {
                values = modbusService.readHoldingRegisters(4001, 10);
                if (values != null) {
                    System.out.println("DEBUG: 读取到Modbus寄存器值: " + java.util.Arrays.toString(values));
                } else {
                    System.out.println("DEBUG: 读取Modbus寄存器失败，返回null");
                }
            }
            
            if (values != null && values.length >= 10) {
                status.put("wmsMode", values[0]); // 4001
                status.put("wmsBusy", values[1]); // 4002
                status.put("wmsOutboundProgress", values[2]); // 4003
                status.put("wmsInboundProgress", values[3]); // 4004
                status.put("wmsOutboundComplete", values[4]); // 4005
                status.put("wmsInboundComplete", values[5]); // 4006
                status.put("mesOutboundOrder", values[6]); // 4007
                status.put("mesInboundOrder", values[7]); // 4008
                // 确保当前执行位置只有在真正执行时才显示
                int currentRow = values[8]; // 4009
                int currentColumn = values[9]; // 4010
                System.out.println("DEBUG: 当前执行位置 - 行:" + currentRow + ", 列:" + currentColumn);
                System.out.println("DEBUG: WMS忙状态:" + values[1] + ", 出库中:" + values[2] + ", 入库中:" + values[3]);
                // 只有在WMS忙状态或正在执行操作时才显示当前位置
                if (values[1] == 1 || values[2] == 1 || values[3] == 1) {
                    status.put("wmsCurrentRow", currentRow);
                    status.put("wmsCurrentColumn", currentColumn);
                    System.out.println("DEBUG: 显示执行位置 - 行:" + currentRow + ", 列:" + currentColumn);
                } else {
                    status.put("wmsCurrentRow", 0);
                    status.put("wmsCurrentColumn", 0);
                    System.out.println("DEBUG: 不显示执行位置，设置为0");
                }
            } else {
                // 如果Modbus连接失败，使用默认值
                status.put("wmsMode", 0);
                status.put("wmsBusy", 0);
                status.put("wmsOutboundProgress", 0);
                status.put("wmsInboundProgress", 0);
                status.put("wmsOutboundComplete", 0);
                status.put("wmsInboundComplete", 0);
                status.put("mesOutboundOrder", 0);
                status.put("mesInboundOrder", 0);
                status.put("wmsCurrentRow", 0);
                status.put("wmsCurrentColumn", 0);
                System.out.println("DEBUG: 使用默认值，所有状态为0");
            }
            
            // 直接从MySQL获取库位状态
            List<WarehouseLocation> locations = locationRepository.findByWarehouseCodeOrderByRowNumberAscColumnNumberAsc("A");
            Map<String, Integer> locationStatus = new HashMap<>();
            
            // 初始化所有25个库位为0（空）
            for (int i = 1; i <= 25; i++) {
                locationStatus.put("location" + i, 0);
            }
            
            // 根据MySQL数据更新库位状态
            for (WarehouseLocation location : locations) {
                int positionIndex = (location.getRowNumber() - 1) * 6 + location.getColumnNumber();
                if (positionIndex >= 1 && positionIndex <= 25) {
                    locationStatus.put("location" + positionIndex, location.getHasPallet() ? 1 : 0);
                }
            }
            
            status.put("locationStatus", locationStatus);
            
        } catch (Exception e) {
            status.put("connected", false);
            status.put("message", "获取MES-WMS状态失败: " + e.getMessage());
        }
        
        return status;
    }
    
    /**
     * 检查Modbus连接状态
     */
    public boolean isModbusConnected() {
        return modbusService.isConnected();
    }
    
    /**
     * 强制重置所有寄存器
     */
    public void forceResetAllRegisters() {
        if (!modbusService.isConnected()) {
            System.out.println("DEBUG: Modbus连接未建立，无法重置寄存器");
            return;
        }
        
        try {
            System.out.println("DEBUG: 开始强制重置MES-WMS寄存器...");
            
            // 重置MES-WMS交互寄存器，但保持WMS_MODE为1（远程模式）
            modbusService.writeSingleRegister(WMS_MODE, 1); // WMS模式保持为远程模式
            modbusService.writeSingleRegister(WMS_BUSY, 0); // WMS是否在忙
            modbusService.writeSingleRegister(WMS_OUTBOUND_PROGRESS, 0); // WMS出库中
            modbusService.writeSingleRegister(WMS_INBOUND_PROGRESS, 0); // WMS入库中
            modbusService.writeSingleRegister(WMS_OUTBOUND_COMPLETE, 0); // WMS出库完成
            modbusService.writeSingleRegister(WMS_INBOUND_COMPLETE, 0); // WMS入库完成
            modbusService.writeSingleRegister(MES_OUTBOUND_ORDER, 0); // MES下单出库
            modbusService.writeSingleRegister(MES_INBOUND_ORDER, 0); // MES下单入库
            modbusService.writeSingleRegister(WMS_CURRENT_ROW, 0); // WMS当前执行的行
            modbusService.writeSingleRegister(WMS_CURRENT_COLUMN, 0); // WMS当前执行的列
            
            // 重置库位状态寄存器 (4011-4035)
            for (int i = LOCATION_STATUS_START; i <= 4035; i++) {
                boolean success = modbusService.writeSingleRegister(i, 0);
                if (!success) {
                    System.out.println("DEBUG: 重置库位状态寄存器 " + i + " 失败");
                }
            }
            
            System.out.println("DEBUG: MES-WMS寄存器已重置，WMS_MODE保持为1（远程模式）");
        } catch (Exception e) {
            System.err.println("强制重置寄存器失败: " + e.getMessage());
        }
    }
}
