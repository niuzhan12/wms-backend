package com.wms.service;

import com.wms.entity.DeviceStatus;
import com.wms.repository.DeviceStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;

@Service
public class DeviceService {
    
    @Autowired
    private DeviceStatusRepository deviceStatusRepository;
    
    // 获取设备状态
    public DeviceStatus getDeviceStatus() {
        DeviceStatus status = deviceStatusRepository.findFirstByOrderByIdAsc();
        if (status == null) {
            status = new DeviceStatus();
            status = deviceStatusRepository.save(status);
        }
        return status;
    }
    
    // 更新设备状态
    public DeviceStatus updateDeviceStatus(DeviceStatus deviceStatus) {
        return deviceStatusRepository.save(deviceStatus);
    }
    
    // 设置设备空闲
    public void setDeviceIdle() {
        DeviceStatus status = getDeviceStatus();
        status.setStatus(DeviceStatus.Status.IDLE);
        status.setCurrentRow(0);
        status.setCurrentColumn(0);
        deviceStatusRepository.save(status);
    }
    
    // 设置设备忙
    public void setDeviceBusy(Integer row, Integer column) {
        DeviceStatus status = getDeviceStatus();
        status.setStatus(DeviceStatus.Status.BUSY);
        status.setCurrentRow(row);
        status.setCurrentColumn(column);
        deviceStatusRepository.save(status);
    }
    
    // 切换控制模式
    public void toggleControlMode() {
        DeviceStatus status = getDeviceStatus();
        if (status.getControlMode() == DeviceStatus.ControlMode.LOCAL) {
            status.setControlMode(DeviceStatus.ControlMode.REMOTE);
        } else {
            status.setControlMode(DeviceStatus.ControlMode.LOCAL);
        }
        deviceStatusRepository.save(status);
    }
    
    // 线体控制
    public Map<String, Object> lineControl(String operation, Integer row, Integer column) {
        Map<String, Object> result = new HashMap<>();
        
        switch (operation) {
            case "inbound":
                result.put("success", true);
                result.put("message", "线体入货成功");
                setDeviceBusy(row, column);
                break;
            case "outbound":
                result.put("success", true);
                result.put("message", "线体出货成功");
                setDeviceBusy(row, column);
                break;
            case "reset":
                result.put("success", true);
                result.put("message", "线体复位成功");
                setDeviceIdle();
                break;
            default:
                result.put("success", false);
                result.put("message", "未知操作");
        }
        
        return result;
    }
}
