-- 创建仓库位置表
CREATE TABLE IF NOT EXISTS warehouse_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_code VARCHAR(10) NOT NULL,
    row_number INT NOT NULL,
    column_number INT NOT NULL,
    has_pallet BOOLEAN DEFAULT FALSE,
    material_status VARCHAR(20) DEFAULT 'EMPTY',
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
    material_status VARCHAR(20) DEFAULT 'RAW',
    quantity INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建设备状态表
CREATE TABLE IF NOT EXISTS device_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(20) DEFAULT 'IDLE',
    current_row INT DEFAULT 0,
    current_col INT DEFAULT 0,
    control_mode VARCHAR(20) DEFAULT 'LOCAL',
    warehouse_selection VARCHAR(50) DEFAULT '未选择',
    command TEXT,
    remote_mode BOOLEAN DEFAULT FALSE,
    remote_working BOOLEAN DEFAULT FALSE,
    file_executing BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 插入仓库位置数据
INSERT INTO warehouse_locations (warehouse_code, row_number, column_number, has_pallet, material_status) VALUES
('A', 1, 1, FALSE, 'EMPTY'),
('A', 1, 2, FALSE, 'EMPTY'),
('A', 1, 3, FALSE, 'EMPTY'),
('A', 1, 4, FALSE, 'EMPTY'),
('A', 1, 5, FALSE, 'EMPTY'),
('A', 1, 6, FALSE, 'EMPTY'),
('A', 2, 1, FALSE, 'EMPTY'),
('A', 2, 2, FALSE, 'EMPTY'),
('A', 2, 3, FALSE, 'EMPTY'),
('A', 2, 4, FALSE, 'EMPTY'),
('A', 2, 5, FALSE, 'EMPTY'),
('A', 2, 6, FALSE, 'EMPTY'),
('A', 3, 1, FALSE, 'EMPTY'),
('A', 3, 2, FALSE, 'EMPTY'),
('A', 3, 3, FALSE, 'EMPTY'),
('A', 3, 4, FALSE, 'EMPTY'),
('A', 3, 5, FALSE, 'EMPTY'),
('A', 3, 6, FALSE, 'EMPTY'),
('A', 4, 1, FALSE, 'EMPTY'),
('A', 4, 2, FALSE, 'EMPTY'),
('A', 4, 3, FALSE, 'EMPTY'),
('A', 4, 4, FALSE, 'EMPTY'),
('A', 4, 5, FALSE, 'EMPTY'),
('A', 4, 6, FALSE, 'EMPTY'),
('B', 1, 1, FALSE, 'EMPTY'),
('B', 1, 2, FALSE, 'EMPTY'),
('B', 1, 3, FALSE, 'EMPTY'),
('B', 1, 4, FALSE, 'EMPTY'),
('B', 1, 5, FALSE, 'EMPTY'),
('B', 1, 6, FALSE, 'EMPTY'),
('B', 2, 1, FALSE, 'EMPTY'),
('B', 2, 2, FALSE, 'EMPTY'),
('B', 2, 3, FALSE, 'EMPTY'),
('B', 2, 4, FALSE, 'EMPTY'),
('B', 2, 5, FALSE, 'EMPTY'),
('B', 2, 6, FALSE, 'EMPTY'),
('B', 3, 1, FALSE, 'EMPTY'),
('B', 3, 2, FALSE, 'EMPTY'),
('B', 3, 3, FALSE, 'EMPTY'),
('B', 3, 4, FALSE, 'EMPTY'),
('B', 3, 5, FALSE, 'EMPTY'),
('B', 3, 6, FALSE, 'EMPTY'),
('B', 4, 1, FALSE, 'EMPTY'),
('B', 4, 2, FALSE, 'EMPTY'),
('B', 4, 3, FALSE, 'EMPTY'),
('B', 4, 4, FALSE, 'EMPTY'),
('B', 4, 5, FALSE, 'EMPTY'),
('B', 4, 6, FALSE, 'EMPTY');

-- 插入示例托盘数据
INSERT INTO pallets (pallet_number, pallet_name, qr_code, warehouse_code, row_number, column_number, is_occupied) VALUES
('P001', '托盘1', 'QR_P001', 'A', 1, 1, TRUE),
('P002', '托盘2', 'QR_P002', 'A', 1, 2, TRUE),
('P003', '托盘3', 'QR_P003', 'A', 2, 1, TRUE),
('P004', '托盘4', 'QR_P004', 'B', 1, 1, TRUE),
('P005', '托盘5', 'QR_P005', 'B', 2, 1, TRUE);

-- 插入示例货物数据
INSERT INTO goods (goods_number, goods_name, qr_code, warehouse_code, row_number, column_number, material_status, quantity) VALUES
('1000', '电机轴毛坯', 'QR_1000', 'A', 1, 1, 'RAW', 5),
('1001', '螺栓毛坯', 'QR_1001', 'A', 1, 2, 'RAW', 6),
('2000', '电机轴成品', 'QR_2000', 'A', 2, 1, 'FINISHED', 1),
('2001', '螺栓成品', 'QR_2001', 'B', 1, 1, 'FINISHED', 3),
('2002', '组装件', 'QR_2002', 'B', 2, 1, 'FINISHED', 2);

-- 插入设备状态数据
INSERT INTO device_status (status, current_row, current_col, control_mode, warehouse_selection) VALUES
('IDLE', 0, 0, 'LOCAL', '未选择');








