-- 创建操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    level VARCHAR(20) NOT NULL,
    message TEXT,
    module VARCHAR(50),
    operation VARCHAR(100),
    user_id BIGINT,
    ip_address VARCHAR(45),
    INDEX idx_timestamp (timestamp),
    INDEX idx_module (module),
    INDEX idx_level (level),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


