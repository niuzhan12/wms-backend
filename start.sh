#!/bin/bash

echo "启动仓储AS/AR软件系统..."
echo

echo "1. 启动后端Spring Boot服务..."
cd backend
gnome-terminal --title="Spring Boot Backend" -- bash -c "mvn spring-boot:run; exec bash" &
cd ..

echo "等待后端服务启动..."
sleep 10

echo "2. 启动前端Vue.js服务..."
cd frontend
gnome-terminal --title="Vue.js Frontend" -- bash -c "npm install && npm run dev; exec bash" &
cd ..

echo
echo "系统启动完成！"
echo "后端服务: http://localhost:8080"
echo "前端服务: http://localhost:3000"
echo "H2数据库控制台: http://localhost:8080/h2-console"
echo
echo "按任意键退出..."
read -n 1
