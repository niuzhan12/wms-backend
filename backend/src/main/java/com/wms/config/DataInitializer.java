package com.wms.config;

import com.wms.entity.*;
import com.wms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private WarehouseLocationRepository locationRepository;
    
    @Autowired
    private PalletRepository palletRepository;
    
    @Autowired
    private GoodsRepository goodsRepository;
    
    @Autowired
    private DeviceStatusRepository deviceStatusRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // 只在数据库为空时初始化数据
        if (locationRepository.count() == 0) {
            // 初始化仓库位置
            initializeWarehouseLocations();
            
            // 初始化托盘数据
            initializePallets();
            
            // 初始化货物数据
            initializeGoods();
            
            // 初始化设备状态
            initializeDeviceStatus();
            
            // 初始化用户数据
            initializeUsers();
            
            System.out.println("数据库初始化完成！");
        } else {
            System.out.println("数据库已存在数据，跳过初始化。");
        }
    }
    
    private void initializeWarehouseLocations() {
        if (locationRepository.count() == 0) {
            // 创建A库和B库，每个4行6列
            for (String warehouseCode : Arrays.asList("A", "B")) {
                for (int row = 1; row <= 4; row++) {
                    for (int col = 1; col <= 6; col++) {
                        WarehouseLocation location = new WarehouseLocation();
                        location.setWarehouseCode(warehouseCode);
                        location.setRowNumber(row);
                        location.setColumnNumber(col);
                        location.setHasPallet(false);
                        location.setMaterialStatus(WarehouseLocation.MaterialStatus.EMPTY);
                        location.setRemarks("");
                        locationRepository.save(location);
                    }
                }
            }
        }
    }
    
    private void initializePallets() {
        if (palletRepository.count() == 0) {
            // 创建示例托盘
            for (int i = 1; i <= 15; i++) {
                Pallet pallet = new Pallet();
                pallet.setPalletNumber("P" + String.format("%03d", i));
                pallet.setPalletName("托盘" + i);
                pallet.setQrCode("QR_P" + String.format("%03d", i));
                pallet.setWarehouseCode("A");
                pallet.setRowNumber((i - 1) / 5 + 1);
                pallet.setColumnNumber((i - 1) % 5 + 1);
                pallet.setIsOccupied(true);
                palletRepository.save(pallet);
            }
        }
    }
    
    private void initializeGoods() {
        if (goodsRepository.count() == 0) {
            // 创建示例货物
            Goods goods1 = new Goods();
            goods1.setGoodsNumber("1000");
            goods1.setGoodsName("电机轴毛坯");
            goods1.setQrCode("QR_1000");
            goods1.setWarehouseCode("A");
            goods1.setRowNumber(2);
            goods1.setColumnNumber(1);
            goods1.setMaterialStatus(Goods.MaterialStatus.RAW);
            goods1.setQuantity(5);
            goodsRepository.save(goods1);
            
            Goods goods2 = new Goods();
            goods2.setGoodsNumber("1001");
            goods2.setGoodsName("螺栓毛坯");
            goods2.setQrCode("QR_1001");
            goods2.setWarehouseCode("A");
            goods2.setRowNumber(3);
            goods2.setColumnNumber(1);
            goods2.setMaterialStatus(Goods.MaterialStatus.RAW);
            goods2.setQuantity(6);
            goodsRepository.save(goods2);
            
            Goods goods3 = new Goods();
            goods3.setGoodsNumber("2000");
            goods3.setGoodsName("电机轴成品");
            goods3.setQrCode("QR_2000");
            goods3.setWarehouseCode("A");
            goods3.setRowNumber(3);
            goods3.setColumnNumber(1);
            goods3.setMaterialStatus(Goods.MaterialStatus.FINISHED);
            goods3.setQuantity(1);
            goodsRepository.save(goods3);
        }
    }
    
    private void initializeDeviceStatus() {
        if (deviceStatusRepository.count() == 0) {
            DeviceStatus status = new DeviceStatus();
            status.setStatus(DeviceStatus.Status.IDLE);
            status.setCurrentRow(0);
            status.setCurrentColumn(0);
            status.setControlMode(DeviceStatus.ControlMode.LOCAL);
            status.setWarehouseSelection("未选择");
            status.setCommand("");
            status.setRemoteMode(false);
            status.setRemoteWorking(false);
            status.setFileExecuting(false);
            deviceStatusRepository.save(status);
        }
    }
    
    private void initializeUsers() {
        if (userRepository.count() == 0) {
            // 创建管理员用户
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123"); // 使用明文密码
            admin.setRole(User.UserRole.ADMIN);
            userRepository.save(admin);
            
            // 创建普通用户
            User user = new User();
            user.setUsername("user");
            user.setPassword("user123"); // 使用明文密码
            user.setRole(User.UserRole.USER);
            userRepository.save(user);
            
            System.out.println("默认用户创建完成：");
            System.out.println("管理员 - 用户名: admin, 密码: admin123");
            System.out.println("普通用户 - 用户名: user, 密码: user123");
        }
    }
}
