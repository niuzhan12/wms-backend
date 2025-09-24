package com.wms.controller;

import com.wms.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户名和密码不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        Map<String, Object> result = authService.login(username, password);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> tokenRequest) {
        String token = tokenRequest.get("token");
        
        if (token == null || token.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        Map<String, Object> result = authService.validateToken(token);
        return ResponseEntity.ok(result);
    }
}
