package com.wms.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "device_status")
public class DeviceStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "device_status")
    private Status status = Status.IDLE; // 设备状态
    
    @Column(name = "current_row")
    private Integer currentRow = 0; // 当前运行的行
    
    @Column(name = "current_column")
    private Integer currentColumn = 0; // 当前运行的列
    
    @Enumerated(EnumType.STRING)
    @Column(name = "control_mode")
    private ControlMode controlMode = ControlMode.LOCAL; // 控制模式
    
    @Column(name = "warehouse_selection")
    private String warehouseSelection; // 仓库选择
    
    @Column(name = "command")
    private String command; // 命令
    
    @Column(name = "remote_mode")
    private Boolean remoteMode = false; // 远程模式
    
    @Column(name = "remote_working")
    private Boolean remoteWorking = false; // 远程工作中
    
    @Column(name = "file_executing")
    private Boolean fileExecuting = false; // 文件执行中
    
    public enum Status {
        IDLE,   // 空闲
        BUSY    // 忙
    }
    
    public enum ControlMode {
        LOCAL,  // 本地
        REMOTE  // 远程
    }
}
