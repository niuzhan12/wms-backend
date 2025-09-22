@echo off
echo 启动后端服务
echo ============
echo.

echo 1. 检查数据库连接...
mysql -u root -p123456 -e "USE wms_db; SELECT COUNT(*) as user_count FROM users;" >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 数据库连接失败或用户表不存在
    echo 请先运行 test_database.bat 设置数据库
    pause
    exit /b 1
)

echo 数据库连接正常
echo.

echo 2. 启动后端服务...
cd backend
java -jar target/warehouse-management-system-1.0.0.jar













