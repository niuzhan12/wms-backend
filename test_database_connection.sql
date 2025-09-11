-- 测试数据库连接
-- 请确保MySQL服务正在运行，并且有以下配置：
-- 用户名: root
-- 密码: 123456
-- 数据库: wms_db

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS wms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE wms_db;

-- 检查数据库是否存在
SELECT 'Database connection test successful' as status;




