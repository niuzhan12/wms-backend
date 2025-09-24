package com.wms.service;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;

@Service
public class StackerModbusService {
    
    @Value("${stacker.ip:127.0.0.1}")
    private String stackerIp;
    
    @Value("${stacker.port:503}")
    private int stackerPort;
    
    @Value("${stacker.slaveId:1}")
    private int stackerSlaveId;
    
    @Value("${stacker.timeout:10000}")
    private int timeout;
    
    @Value("${stacker.retries:3}")
    private int retries;
    
    @Value("${stacker.heartbeat.interval:30000}")
    private int heartbeatInterval;
    
    private TCPMasterConnection connection;
    private boolean connected = false;
    private volatile boolean isConnecting = false;
    private long lastHeartbeat = 0;
    
    // 堆垛机Modbus寄存器地址定义
    // WMS -> 堆垛机寄存器 (4001-4010)
    public static final int STACKER_MODE = 4001;             // 堆垛机的模式 (0=本地, 1=远程)
    public static final int STACKER_STATUS = 4002;           // 堆垛机是否在忙 (0=空闲, 1=忙)
    public static final int STACKER_OUTBOUND_PROGRESS = 4003; // 堆垛机出库中 (0=空闲, 1=出库中)
    public static final int STACKER_INBOUND_PROGRESS = 4004;  // 堆垛机入库中 (0=空闲, 1=入库中)
    public static final int STACKER_OUTBOUND_COMPLETE = 4005; // 堆垛机出库完成 (0=空闲, 1=完成)
    public static final int STACKER_INBOUND_COMPLETE = 4006;  // 堆垛机入库完成 (0=空闲, 1=完成)
    public static final int WMS_OUTBOUND_ORDER = 4007;       // WMS下单出库 (0=空闲, 1=出库)
    public static final int WMS_INBOUND_ORDER = 4008;        // WMS下单入库 (0=空闲, 1=入库)
    public static final int WMS_ORDER_ROW = 4009;            // WMS下单行 (0=空闲, 实际行)
    public static final int WMS_ORDER_COLUMN = 4010;         // WMS下单列 (0=空闲, 实际列)
    
    // 注意：我们直接使用4001-4010寄存器，不需要额外的反馈寄存器
    // 堆垛机状态通过4002寄存器直接反馈给WMS
    
    @PostConstruct
    public void initConnection() {
        connectWithRetry();
        startHeartbeat();
    }
    
    /**
     * 带重试的连接方法
     */
    private void connectWithRetry() {
        if (isConnecting) {
            return; // 避免重复连接
        }
        
        isConnecting = true;
        int attempts = 0;
        
        while (attempts < retries) {
            try {
                if (connection != null && connection.isConnected()) {
                    connection.close();
                }
                
                connection = new TCPMasterConnection(InetAddress.getByName(stackerIp));
                connection.setPort(stackerPort);
                connection.setTimeout(timeout);
                connection.connect();
                
                connected = true;
                lastHeartbeat = System.currentTimeMillis();
                System.out.println("堆垛机Modbus连接已建立: " + stackerIp + ":" + stackerPort + " (尝试次数: " + (attempts + 1) + ")");
                break;
                
            } catch (Exception e) {
                attempts++;
                connected = false;
                System.err.println("堆垛机Modbus连接失败 (尝试 " + attempts + "/" + retries + "): " + e.getMessage());
                
                if (attempts < retries) {
                    try {
                        Thread.sleep(2000); // 等待2秒后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        isConnecting = false;
    }
    
    /**
     * 启动连接监控（不自动重连）
     */
    private void startHeartbeat() {
        Thread heartbeatThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(heartbeatInterval);
                    
                    // 只监控连接状态，不自动重连
                    if (isConnected()) {
                        try {
                            // 简单的连接验证，不执行实际读取操作
                            lastHeartbeat = System.currentTimeMillis();
                        } catch (Exception e) {
                            System.err.println("堆垛机连接验证失败: " + e.getMessage());
                        }
                    } else {
                        System.out.println("堆垛机连接已断开，等待手动重连");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        heartbeatThread.setDaemon(true);
        heartbeatThread.setName("StackerModbus-Monitor");
        heartbeatThread.start();
    }
    
    @PreDestroy
    public void closeConnection() {
        if (connection != null && connection.isConnected()) {
            connection.close();
            connected = false;
            System.out.println("堆垛机Modbus连接已关闭");
        }
    }
    
    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return connected && connection != null && connection.isConnected();
    }
    
    /**
     * 重新连接
     */
    public void reconnect() {
        closeConnection();
        initConnection();
    }
    
    /**
     * 读取单个寄存器
     */
    public int readSingleRegister(int address) throws Exception {
        if (!isConnected()) {
            throw new Exception("堆垛机Modbus连接未建立");
        }
        
        try {
            ReadMultipleRegistersRequest request = new ReadMultipleRegistersRequest(address, 1);
            request.setUnitID(stackerSlaveId);
            
            ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
            transaction.setRequest(request);
            transaction.execute();
            
            ReadMultipleRegistersResponse response = (ReadMultipleRegistersResponse) transaction.getResponse();
            return response.getRegisterValue(0);
        } catch (ModbusException e) {
            throw new Exception("读取寄存器失败: " + e.getMessage());
        }
    }
    
    /**
     * 读取多个寄存器
     */
    public int[] readHoldingRegisters(int address, int quantity) throws Exception {
        if (!isConnected()) {
            throw new Exception("堆垛机Modbus连接未建立");
        }
        
        try {
            ReadMultipleRegistersRequest request = new ReadMultipleRegistersRequest(address, quantity);
            request.setUnitID(stackerSlaveId);
            
            ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
            transaction.setRequest(request);
            transaction.execute();
            
            ReadMultipleRegistersResponse response = (ReadMultipleRegistersResponse) transaction.getResponse();
            int[] values = new int[quantity];
            for (int i = 0; i < quantity; i++) {
                values[i] = response.getRegisterValue(i);
            }
            return values;
        } catch (ModbusException e) {
            throw new Exception("读取寄存器失败: " + e.getMessage());
        }
    }
    
    /**
     * 写入单个寄存器
     */
    public void writeSingleRegister(int address, int value) throws Exception {
        if (!isConnected()) {
            throw new Exception("堆垛机Modbus连接未建立");
        }
        
        try {
            WriteSingleRegisterRequest request = new WriteSingleRegisterRequest();
            request.setReference(address);
            request.setRegister(new SimpleRegister(value));
            request.setUnitID(stackerSlaveId);
            
            ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
            transaction.setRequest(request);
            transaction.execute();
        } catch (ModbusException e) {
            throw new Exception("写入寄存器失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取堆垛机状态
     */
    public int getStackerStatus() {
        try {
            // 直接读取堆垛机状态寄存器 (4002)
            int stackerStatus = readSingleRegister(STACKER_STATUS);
            
            // 如果状态寄存器为0，但正在进行操作，则返回忙碌状态
            if (stackerStatus == 0) {
                int inboundProgress = readSingleRegister(STACKER_INBOUND_PROGRESS);
                int outboundProgress = readSingleRegister(STACKER_OUTBOUND_PROGRESS);
                
                // 如果有进行中的操作，返回忙碌状态
                if (inboundProgress == 1 || outboundProgress == 1) {
                    return 1; // 忙碌
                }
            }
            
            return stackerStatus;
        } catch (Exception e) {
            System.err.println("读取堆垛机状态失败: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 获取堆垛机操作类型
     */
    public int getStackerOperation() {
        try {
            // 检查是入库还是出库
            int inboundProgress = readSingleRegister(STACKER_INBOUND_PROGRESS);
            int outboundProgress = readSingleRegister(STACKER_OUTBOUND_PROGRESS);
            if (inboundProgress == 1) return 1; // 入库
            if (outboundProgress == 1) return 2; // 出库
            return 0; // 无操作
        } catch (Exception e) {
            System.err.println("读取堆垛机操作类型失败: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 获取堆垛机当前位置
     */
    public int[] getStackerPosition() {
        try {
            int row = readSingleRegister(WMS_ORDER_ROW);
            int column = readSingleRegister(WMS_ORDER_COLUMN);
            return new int[]{row, column};
        } catch (Exception e) {
            System.err.println("读取堆垛机位置失败: " + e.getMessage());
            return new int[]{0, 0};
        }
    }
    
    /**
     * 获取堆垛机进度
     */
    public int getStackerProgress() {
        try {
            // 根据操作状态返回进度
            int inboundProgress = readSingleRegister(STACKER_INBOUND_PROGRESS);
            int outboundProgress = readSingleRegister(STACKER_OUTBOUND_PROGRESS);
            
            if (inboundProgress == 1 || outboundProgress == 1) {
                // 检查是否有完成标志
                int inboundComplete = readSingleRegister(STACKER_INBOUND_COMPLETE);
                int outboundComplete = readSingleRegister(STACKER_OUTBOUND_COMPLETE);
                
                if (inboundComplete == 1 || outboundComplete == 1) {
                    return 100; // 已完成
                }
                
                return 50; // 假设进行中为50%
            }
            return 0;
        } catch (Exception e) {
            System.err.println("读取堆垛机进度失败: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 获取堆垛机完成状态
     */
    public int getStackerComplete() {
        try {
            int inboundComplete = readSingleRegister(STACKER_INBOUND_COMPLETE);
            int outboundComplete = readSingleRegister(STACKER_OUTBOUND_COMPLETE);
            if (inboundComplete == 1 || outboundComplete == 1) {
                return 1; // 完成
            }
            return 0; // 未完成
        } catch (Exception e) {
            System.err.println("读取堆垛机完成状态失败: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 发送操作指令到堆垛机
     */
    public void sendOperationCommand(int operation, int targetRow, int targetColumn) {
        try {
            // 根据操作类型设置相应的寄存器
            if (operation == 1) { // 入库
                writeSingleRegister(WMS_INBOUND_ORDER, 1); // WMS下单入库
                writeSingleRegister(WMS_ORDER_ROW, targetRow); // WMS下单行
                writeSingleRegister(WMS_ORDER_COLUMN, targetColumn); // WMS下单列
                writeSingleRegister(STACKER_INBOUND_PROGRESS, 1); // 堆垛机入库中
            } else if (operation == 2) { // 出库
                writeSingleRegister(WMS_OUTBOUND_ORDER, 1); // WMS下单出库
                writeSingleRegister(WMS_ORDER_ROW, targetRow); // WMS下单行
                writeSingleRegister(WMS_ORDER_COLUMN, targetColumn); // WMS下单列
                writeSingleRegister(STACKER_OUTBOUND_PROGRESS, 1); // 堆垛机出库中
            }
            writeSingleRegister(STACKER_STATUS, 1); // 堆垛机是否在忙
            System.out.println("已发送操作指令到堆垛机: 操作=" + operation + ", 目标位置=" + targetRow + "行" + targetColumn + "列");
        } catch (Exception e) {
            System.err.println("发送操作指令失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止堆垛机操作
     */
    public void stopStackerOperation() {
        try {
            // 清除进度寄存器
            writeSingleRegister(STACKER_INBOUND_PROGRESS, 0);
            writeSingleRegister(STACKER_OUTBOUND_PROGRESS, 0);
            // 清除完成寄存器
            writeSingleRegister(STACKER_INBOUND_COMPLETE, 0);
            writeSingleRegister(STACKER_OUTBOUND_COMPLETE, 0);
            // 清除位置信息
            writeSingleRegister(WMS_ORDER_ROW, 0);
            writeSingleRegister(WMS_ORDER_COLUMN, 0);
            // 设置为空闲状态
            writeSingleRegister(STACKER_STATUS, 0);
            System.out.println("已停止堆垛机操作");
        } catch (Exception e) {
            System.err.println("停止堆垛机操作失败: " + e.getMessage());
        }
    }
}