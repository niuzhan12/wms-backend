package com.wms.entity;

import javax.persistence.*;

@Entity
@Table(name = "stacker")
public class Stacker {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "ip_address", nullable = false)
    private String ipAddress;
    
    @Column(name = "port", nullable = false)
    private Integer port = 502;
    
    @Column(name = "slave_id", nullable = false)
    private Integer slaveId = 1;
    
    @Column(name = "status")
    private String status = "IDLE"; // IDLE, BUSY, ERROR
    
    @Column(name = "current_row")
    private Integer currentRow = 0;
    
    @Column(name = "current_column")
    private Integer currentColumn = 0;
    
    @Column(name = "control_mode")
    private String controlMode = "REMOTE"; // LOCAL, REMOTE
    
    @Column(name = "is_connected")
    private Boolean isConnected = false;
    
    // 构造函数
    public Stacker() {}
    
    public Stacker(String name, String ipAddress, Integer port, Integer slaveId) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;
        this.slaveId = slaveId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public Integer getPort() {
        return port;
    }
    
    public void setPort(Integer port) {
        this.port = port;
    }
    
    public Integer getSlaveId() {
        return slaveId;
    }
    
    public void setSlaveId(Integer slaveId) {
        this.slaveId = slaveId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getCurrentRow() {
        return currentRow;
    }
    
    public void setCurrentRow(Integer currentRow) {
        this.currentRow = currentRow;
    }
    
    public Integer getCurrentColumn() {
        return currentColumn;
    }
    
    public void setCurrentColumn(Integer currentColumn) {
        this.currentColumn = currentColumn;
    }
    
    public String getControlMode() {
        return controlMode;
    }
    
    public void setControlMode(String controlMode) {
        this.controlMode = controlMode;
    }
    
    public Boolean getIsConnected() {
        return isConnected;
    }
    
    public void setIsConnected(Boolean isConnected) {
        this.isConnected = isConnected;
    }
}
