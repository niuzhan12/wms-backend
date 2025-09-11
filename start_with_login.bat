@echo off
echo 启动仓储管理系统（带登录功能）
echo.

echo 启动后端服务...
cd backend
start "Backend" cmd /k "mvn spring-boot:run"
cd ..

echo 等待后端启动...
timeout /t 10 /nobreak > nul

echo 启动前端服务...
cd frontend
start "Frontend" cmd /k "npm run dev"
cd ..

echo.
echo 系统启动完成！
echo 访问地址: http://localhost:5173
echo.
echo 默认账户：
echo 管理员 - 用户名: admin, 密码: admin123
echo 普通用户 - 用户名: user, 密码: user123
echo.
pause
