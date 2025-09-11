@echo off
echo 启动WMS Modbus Master系统...
echo.

echo 1. 检查Java环境...
java -version
if %errorlevel% neq 0 (
    echo 错误: 未找到Java环境，请先安装Java 8或更高版本
    pause
    exit /b 1
)

echo.
echo 2. 检查Maven环境...
mvn -version
if %errorlevel% neq 0 (
    echo 错误: 未找到Maven环境，请先安装Maven
    pause
    exit /b 1
)

echo.
echo 3. 编译后端项目...
cd backend
mvn clean compile
if %errorlevel% neq 0 (
    echo 错误: 后端编译失败
    pause
    exit /b 1
)

echo.
echo 4. 启动WMS Modbus Master服务...
echo 服务将在 http://localhost:8080 启动
echo 测试页面: test_wms_master.html
echo.
echo 注意: 请使用外部Modbus Slave工具模拟MES
echo 推荐工具: ModbusPal, QModMaster, ModbusSim
echo.
echo 按 Ctrl+C 停止服务
echo.
mvn spring-boot:run

pause




