@echo off
echo ========================================
echo    Modbus TCP 服务器模拟器启动脚本
echo ========================================
echo.

echo 检查Python环境...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到Python环境
    echo 请先安装Python 3.6或更高版本
    pause
    exit /b 1
)

echo Python环境检查通过
echo.

echo 启动Modbus TCP服务器模拟器...
echo 这将启动两个Modbus服务器:
echo - 端口502: MES-WMS服务器
echo - 端口503: WMS-堆垛机服务器
echo.

python modbus_simulator.py

echo.
echo Modbus模拟器已停止
pause

