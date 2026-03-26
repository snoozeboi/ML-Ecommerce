package com.shop.ecommerce.controller;

import com.shop.ecommerce.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{userId}")
    public Map<String, Object> getUserProfile(@PathVariable int userId) {
        return userService.getUserProfile(userId);
    }

    @PutMapping("/{userId}")
    public Map<String, Object> updateUserProfile(
            @PathVariable int userId,
            @RequestBody Map<String, String> updateRequest) {
        String username = updateRequest.get("username");
        String email = updateRequest.get("email");
        
        return userService.updateUserProfile(userId, username, email);
    }
}
