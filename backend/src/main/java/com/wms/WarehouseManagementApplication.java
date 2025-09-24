package com.wms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WarehouseManagementApplication {
    public static void main(String[] args) {    
        System.out.println("=== WMS Backend Starting ===");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
        System.out.println("Environment: " + System.getenv("SPRING_PROFILES_ACTIVE"));
        
        try {
            SpringApplication.run(WarehouseManagementApplication.class, args);
            System.out.println("=== WMS Backend Started Successfully ===");
        } catch (Exception e) {
            System.err.println("=== WMS Backend Failed to Start ===");
            e.printStackTrace();
            throw e;
        }
    }
}




