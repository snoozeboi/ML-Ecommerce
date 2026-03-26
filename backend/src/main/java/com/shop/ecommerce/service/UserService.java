package com.shop.ecommerce.service;

import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@ecommerce.com}")
    private String adminEmail;

    public Map<String, Object> getUserProfile(int userId) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUserName());
            userData.put("email", user.getEmail());
            userData.put("wallet", user.getWalletBalance());
            userData.put("avatar", null); // Frontend expects avatar field
            userData.put("createdAt", user.getCreatedAt());
            
            result.put("success", true);
            result.put("data", userData);
        } else {
            result.put("success", false);
            result.put("message", "User not found");
        }
        
        return result;
    }

    public Map<String, Object> updateUserProfile(int userId, String username, String email) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "User not found");
            return result;
        }
        
        User user = userOpt.get();
        
        // Validate input
        if (username != null && !username.trim().isEmpty()) {
            String normalizedUsername = username.trim();
            
            // Check if username is being changed and if new username already exists
            if (!user.getUserName().equals(normalizedUsername)) {
                if (userRepository.existsByUserName(normalizedUsername)) {
                    result.put("success", false);
                    result.put("message", "Username already taken");
                    return result;
                }
                user.setUserName(normalizedUsername);
            }
        }
        
        if (email != null && !email.trim().isEmpty()) {
            String normalizedEmail = email.trim().toLowerCase();
            String normalizedAdminEmail = adminEmail.trim().toLowerCase();
            
            // Check if this is the admin user trying to change email
            if (user.getEmail().toLowerCase().trim().equals(normalizedAdminEmail)) {
                // Admin cannot change their email
                if (!user.getEmail().equals(normalizedEmail)) {
                    result.put("success", false);
                    result.put("message", "Admin email cannot be changed for security reasons");
                    return result;
                }
            }
            
            // Check if email is being changed and if new email already exists
            if (!user.getEmail().equals(normalizedEmail)) {
                if (userRepository.existsByEmail(normalizedEmail)) {
                    result.put("success", false);
                    result.put("message", "Email already registered");
                    return result;
                }
                user.setEmail(normalizedEmail);
            }
        }
        
        // Update last activity
        user.setLastActivity(LocalDateTime.now());
        
        try {
            // Save to PostgreSQL database
            User updatedUser = userRepository.save(user);
            
            // Prepare response data
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", updatedUser.getId());
            userData.put("username", updatedUser.getUserName());
            userData.put("email", updatedUser.getEmail());
            userData.put("wallet", updatedUser.getWalletBalance());
            userData.put("avatar", null);
            
            result.put("success", true);
            result.put("message", "Profile updated successfully");
            result.put("data", userData);
            
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to update profile: " + e.getMessage());
            e.printStackTrace();
            return result;
        }
    }
}
