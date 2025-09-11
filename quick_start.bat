@echo off
echo 快速启动仓储管理系统
echo.

echo 1. 检查MySQL服务...
net start | findstr "MySQL" >nul
if %errorlevel% neq 0 (
    echo 警告: MySQL服务可能未启动
    echo 请确保MySQL服务正在运行
    echo.
)

echo 2. 启动后端服务...
cd backend
start "Backend" cmd /k "mvn spring-boot:run"
cd ..

echo 3. 等待后端启动（10秒）...
timeout /t 10 /nobreak >nul

echo 4. 启动前端服务...
cd frontend
start "Frontend" cmd /k "npm run dev"
cd ..

echo.
echo 启动完成！
echo 访问地址: http://localhost:5173
echo 后端API: http://localhost:8080
echo.
echo 如果仍有网络错误，请检查：
echo 1. MySQL服务是否启动
echo 2. 数据库用户名密码是否正确（root/123456）
echo 3. 端口8080是否被占用
echo.
pause




