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
    
    @Value("${modbus.timeout:5000}")
    private int timeout;
    
    @Value("${modbus.retries:3}")
    private int retries;
    
    private TCPMasterConnection connection;
    
    @PostConstruct
    public void initConnection() {
        try {
            connection = new TCPMasterConnection(InetAddress.getByName(host));
            connection.setPort(port);
            connection.setTimeout(timeout);
            // connection.setRetries(retries); // 某些版本可能不支持
            connection.connect();
            System.out.println("Modbus连接已建立: " + host + ":" + port);
        } catch (Exception e) {
            System.err.println("Modbus连接失败: " + e.getMessage());
        }
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
    
    /**
     * 重新连接
     */
    public void reconnect() {
        closeConnection();
        initConnection();
    }
}
