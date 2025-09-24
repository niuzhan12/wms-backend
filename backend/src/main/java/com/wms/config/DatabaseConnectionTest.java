package com.wms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseConnectionTest implements CommandLineRunner {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("✅ MySQL数据库连接成功！");
            System.out.println("数据库URL: " + connection.getMetaData().getURL());
            System.out.println("数据库产品名称: " + connection.getMetaData().getDatabaseProductName());
            System.out.println("数据库版本: " + connection.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.err.println("❌ MySQL数据库连接失败！");
            System.err.println("错误信息: " + e.getMessage());
            System.err.println("请检查：");
            System.err.println("1. MySQL服务是否启动");
            System.err.println("2. 数据库wms_db是否存在");
            System.err.println("3. 用户名密码是否正确");
            System.err.println("4. 端口3306是否可访问");
        }
    }
}

