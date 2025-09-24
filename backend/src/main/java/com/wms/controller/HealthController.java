package com.wms.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "WMS Backend");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "WMS Backend is running!");
        response.put("status", "OK");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
