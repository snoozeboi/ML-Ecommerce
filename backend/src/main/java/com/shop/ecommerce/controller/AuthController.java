package com.shop.ecommerce.controller;

import com.shop.ecommerce.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");
        
        return authService.authenticateUser(email, password);
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> registerRequest) {
        String name = registerRequest.get("name");
        String email = registerRequest.get("email");
        String password = registerRequest.get("password");
        
        // Use name as userName if provided, otherwise use email prefix
        String userName;
        if (name != null && !name.trim().isEmpty()) {
            userName = name.trim();
        } else if (email != null && email.contains("@")) {
            userName = email.split("@")[0];
        } else {
            userName = email != null ? email : "user";
        }
        
        return authService.registerUser(userName, email, password);
    }

    @GetMapping("/test-login")
    public Map<String, Object> testLogin(@RequestParam String email, @RequestParam String password) {
        return authService.authenticateUser(email, password);
    }

    @GetMapping("/check-credentials")
    public Map<String, Object> checkCredentials(@RequestParam String email, @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        boolean isValid = authService.isValidCredentials(email, password);
        result.put("valid", isValid);
        result.put("message", isValid ? "Credentials are valid" : "Invalid credentials");
        return result;
    }
} 