package com.wms.service;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;

@Service
public class ModbusService {
    
    @Value("${modbus.host:127.0.0.1}")
    private String host;
    
    @Value("${modbus.port:502}")
    private int port;
    
    @Value("${modbus.slaveId:1}")
    private int slaveId;
    
    @Value("${modbus.timeout:10000}")
    private int timeout;
    
    @Value("${modbus.retries:3}")
    private int retries;
    
    @Value("${modbus.heartbeat.interval:30000}")
    private int heartbeatInterval;
    
    private TCPMasterConnection connection;
    private volatile boolean isConnecting = false;
    private long lastHeartbeat = 0;
    
    @PostConstruct
    public void initConnection() {
        connectWithRetry();
        startHeartbeat();
    }
    
    /**
     * 稳定的连接方法
     */
    private void connectWithRetry() {
        if (isConnecting) {
            return; // 避免重复连接
        }
        
        isConnecting = true;
        
        try {
            // 确保旧连接完全关闭
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    // 忽略关闭异常
                }
                connection = null;
            }
            
            // 创建新连接
            connection = new TCPMasterConnection(InetAddress.getByName(host));
            connection.setPort(port);
            connection.setTimeout(timeout);
            
            // 设置连接参数以提高稳定性
            // connection.setRetries(1); // 某些版本可能不支持此方法
            
            connection.connect();
            
            lastHeartbeat = System.currentTimeMillis();
            System.out.println("Modbus连接已建立: " + host + ":" + port);
            
        } catch (Exception e) {
            System.err.println("Modbus连接失败: " + e.getMessage());
            connection = null;
        } finally {
            isConnecting = false;
        }
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
                            System.err.println("连接验证失败: " + e.getMessage());
                        }
                    } else {
                        System.out.println("连接已断开，等待手动重连");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        heartbeatThread.setDaemon(true);
        heartbeatThread.setName("Modbus-Monitor");
        heartbeatThread.start();
    }
    
    /**
     * 重新初始化连接方法
     */
    public void reconnect() {
        connectWithRetry();
    }
    
    @PreDestroy
    public void closeConnection() {
        if (connection != null && connection.isConnected()) {
            connection.close();
            System.out.println("Modbus连接已关闭");
        }
    }
    
    /**
     * 读取保持寄存器
     */
    public int[] readHoldingRegisters(int startAddress, int quantity) {
        return readHoldingRegistersWithRetry(startAddress, quantity, 0);
    }
    
    /**
     * 带重试的读取保持寄存器
     */
    private int[] readHoldingRegistersWithRetry(int startAddress, int quantity, int retryCount) {
        if (connection == null || !connection.isConnected()) {
            System.err.println("Modbus连接未建立");
            return null;
        }
        
        try {
            ReadMultipleRegistersRequest request = new ReadMultipleRegistersRequest(startAddress, quantity);
            request.setUnitID(slaveId);
            
            ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
            transaction.setRequest(request);
            transaction.execute();
            
            ReadMultipleRegistersResponse response = (ReadMultipleRegistersResponse) transaction.getResponse();
            int[] values = new int[quantity];
            for (int i = 0; i < quantity; i++) {
                values[i] = response.getRegister(i).getValue();
            }
            
            lastHeartbeat = System.currentTimeMillis();
            return values;
            
        } catch (ModbusException e) {
            System.err.println("读取寄存器失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 写入单个寄存器
     */
    public boolean writeSingleRegister(int address, int value) {
        return writeSingleRegisterWithRetry(address, value, 0);
    }
    
    /**
     * 带重试的写入单个寄存器
     */
    private boolean writeSingleRegisterWithRetry(int address, int value, int retryCount) {
        if (connection == null || !connection.isConnected()) {
            System.err.println("Modbus连接未建立");
            return false;
        }
        
        try {
            WriteSingleRegisterRequest request = new WriteSingleRegisterRequest();
            request.setReference(address);
            request.setRegister(new com.ghgande.j2mod.modbus.procimg.SimpleRegister(value));
            request.setUnitID(slaveId);
            
            ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
            transaction.setRequest(request);
            transaction.execute();
            
            lastHeartbeat = System.currentTimeMillis();
            return true;
            
        } catch (ModbusException e) {
            System.err.println("写入寄存器失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return connection != null && connection.isConnected();
    }
    
    /**
     * 连接到指定主机
     */
    public void connect(String host, int port, int slaveId) {
        try {
            closeConnection();
            
            this.host = host;
            this.port = port;
            this.slaveId = slaveId;
            
            connection = new TCPMasterConnection(InetAddress.getByName(host));
            connection.setPort(port);
            connection.setTimeout(timeout);
            connection.connect();
            System.out.println("Modbus连接已建立: " + host + ":" + port + " (从站ID:" + slaveId + ")");
        } catch (Exception e) {
            System.err.println("Modbus连接失败: " + e.getMessage());
        }
    }
    
}
