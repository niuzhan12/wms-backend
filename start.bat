@echo off
echo 启动仓储AS/AR软件系统...
echo.

echo 1. 启动后端Spring Boot服务...
cd backend
start "Spring Boot Backend" cmd /k "mvn spring-boot:run"
cd ..

echo 等待后端服务启动...
timeout /t 10 /nobreak > nul

echo 2. 启动前端Vue.js服务...
cd frontend
start "Vue.js Frontend" cmd /k "npm install && npm run dev"
cd ..

echo.
echo 系统启动完成！
echo 后端服务: http://localhost:8080
echo 前端服务: http://localhost:3000
echo H2数据库控制台: http://localhost:8080/h2-console
echo.
echo 按任意键退出...
pause > nul
