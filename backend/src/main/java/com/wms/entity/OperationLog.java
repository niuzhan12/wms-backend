package com.wms.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_logs")
public class OperationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "level", nullable = false, length = 20)
    private String level;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "module", length = 50)
    private String module;
    
    @Column(name = "operation", length = 100)
    private String operation;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    // 构造函数
    public OperationLog() {}
    
    public OperationLog(String level, String message, String module, String operation) {
        this.timestamp = LocalDateTime.now();
        this.level = level;
        this.message = message;
        this.module = module;
        this.operation = operation;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getModule() {
        return module;
    }
    
    public void setModule(String module) {
        this.module = module;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}


