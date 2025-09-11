-- 初始化数据库
-- 请先确保MySQL服务正在运行

-- 创建数据库
CREATE DATABASE IF NOT EXISTS wms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE wms_db;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'USER') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建仓库位置表
CREATE TABLE IF NOT EXISTS warehouse_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_code VARCHAR(10) NOT NULL,
    row_number INT NOT NULL,
    column_number INT NOT NULL,
    has_pallet BOOLEAN DEFAULT FALSE,
    material_status ENUM('EMPTY', 'OCCUPIED', 'RESERVED') DEFAULT 'EMPTY',
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_location (warehouse_code, row_number, column_number)
);

-- 创建托盘表
CREATE TABLE IF NOT EXISTS pallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pallet_number VARCHAR(20) NOT NULL UNIQUE,
    pallet_name VARCHAR(100),
    qr_code VARCHAR(50),
    warehouse_code VARCHAR(10),
    row_number INT,
    column_number INT,
    is_occupied BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建货物表
CREATE TABLE IF NOT EXISTS goods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goods_number VARCHAR(20) NOT NULL UNIQUE,
    goods_name VARCHAR(100),
    qr_code VARCHAR(50),
    warehouse_code VARCHAR(10),
    row_number INT,
    column_number INT,
    material_status ENUM('RAW', 'PROCESSING', 'FINISHED') DEFAULT 'RAW',
    quantity INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建设备状态表
CREATE TABLE IF NOT EXISTS device_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status ENUM('IDLE', 'WORKING', 'ERROR', 'MAINTENANCE') DEFAULT 'IDLE',
    current_row INT DEFAULT 0,
    current_column INT DEFAULT 0,
    control_mode ENUM('LOCAL', 'REMOTE') DEFAULT 'LOCAL',
    warehouse_selection VARCHAR(50) DEFAULT '未选择',
    command TEXT,
    remote_mode BOOLEAN DEFAULT FALSE,
    remote_working BOOLEAN DEFAULT FALSE,
    file_executing BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建Modbus设备表
CREATE TABLE IF NOT EXISTS modbus_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_name VARCHAR(100) NOT NULL,
    device_type VARCHAR(50),
    connection_type ENUM('TCP', 'RTU') NOT NULL,
    host VARCHAR(100),
    port INT,
    slave_id INT NOT NULL,
    device_status ENUM('ONLINE', 'OFFLINE', 'ERROR') DEFAULT 'OFFLINE',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 插入默认用户
INSERT IGNORE INTO users (username, password, role) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'ADMIN'),
('user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'USER');

-- 显示创建结果
SELECT 'Database initialization completed successfully!' as status;
SELECT COUNT(*) as user_count FROM users;
SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = 'wms_db';




