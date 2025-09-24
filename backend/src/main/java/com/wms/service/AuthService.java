package com.wms.service;

import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "用户名不存在");
            return result;
        }
        
        User user = userOpt.get();
        if (!password.equals(user.getPassword())) {
            result.put("success", false);
            result.put("message", "密码错误");
            return result;
        }
        
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        
        result.put("success", true);
        result.put("message", "登录成功");
        result.put("token", token);
        result.put("username", user.getUsername());
        result.put("role", user.getRole().name());
        result.put("roleDescription", user.getRole().getDescription());
        
        return result;
    }
    
    public Map<String, Object> validateToken(String token) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            
            if (jwtUtil.validateToken(token, username)) {
                result.put("success", true);
                result.put("username", username);
                result.put("role", role);
            } else {
                result.put("success", false);
                result.put("message", "Token无效或已过期");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Token解析失败");
        }
        
        return result;
    }
}
