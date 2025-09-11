@echo off
echo 测试数据库连接
echo ==============
echo.

echo 测试MySQL连接...
mysql -u root -p123456 -e "SELECT 'Database connection successful!' as status;"
if %errorlevel% equ 0 (
    echo 数据库连接成功！
    echo.
    echo 创建数据库...
    mysql -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS wms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    echo.
    echo 创建用户表...
    mysql -u root -p123456 wms_db < create_users_table.sql
    echo.
    echo 数据库设置完成！
) else (
    echo 数据库连接失败！
    echo.
    echo 可能的原因：
    echo 1. MySQL服务未启动
    echo 2. 用户名或密码错误（当前: root/123456）
    echo 3. MySQL未安装
    echo.
    echo 请检查：
    echo 1. 运行 net start mysql 启动MySQL服务
    echo 2. 确认用户名密码正确
    echo 3. 修改 application.yml 中的数据库配置
)
echo.
pause








