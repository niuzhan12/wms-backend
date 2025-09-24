@echo off
echo 启动完整仓储管理系统
echo ====================
echo.

echo 步骤1: 测试数据库连接
echo --------------------
test_database.bat
if %errorlevel% neq 0 (
    echo 数据库设置失败，请检查MySQL配置
    pause
    exit /b 1
)

echo.
echo 步骤2: 启动后端服务
echo ------------------
start "Backend Service" cmd /k "start_backend.bat"

echo 等待后端服务启动...
timeout /t 15 /nobreak >nul

echo.
echo 步骤3: 启动前端服务
echo ------------------
cd frontend
start "Frontend Service" cmd /k "npm run dev"
cd ..

echo.
echo 系统启动完成！
echo 访问地址: http://localhost:5173
echo 后端API: http://localhost:8080
echo.
echo 默认账户:
echo 管理员: admin / admin123
echo 普通用户: user / user123
echo.
pause















