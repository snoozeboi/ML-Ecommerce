package com.shop.ecommerce.service;

import com.shop.ecommerce.model.User;
import com.shop.ecommerce.model.UserSegment;
import com.shop.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@ecommerce.com}")
    private String adminEmail;

    public Map<String, Object> authenticateUser(String email, String password) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                // Prepare user data matching frontend expectations
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("userName", user.getUserName());
                userData.put("username", user.getUserName()); // Also include for compatibility
                userData.put("email", user.getEmail());
                userData.put("wallet", user.getWalletBalance());
                userData.put("avatar", null); // Frontend expects avatar field
                
                result.put("success", true);
                result.put("message", "Login successful");
                result.put("user", userData);
            } else {
                result.put("success", false);
                result.put("message", "Invalid password");
            }
        } else {
            result.put("success", false);
            result.put("message", "User not found");
        }
        
        return result;
    }

    public boolean isValidCredentials(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPasswordHash());
    }

    public Map<String, Object> registerUser(String userName, String email, String password) {
        Map<String, Object> result = new HashMap<>();
        
        // Validate input first
        if (userName == null || userName.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Username is required");
            return result;
        }
        
        if (email == null || email.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Email is required");
            return result;
        }
        
        if (password == null || password.trim().isEmpty() || password.length() < 6) {
            result.put("success", false);
            result.put("message", "Password must be at least 6 characters");
            return result;
        }
        
        // Normalize email to lowercase for consistency
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedUserName = userName.trim();
        
        // Check if email already exists (using normalized email)
        if (userRepository.existsByEmail(normalizedEmail)) {
            result.put("success", false);
            result.put("message", "Email already registered");
            return result;
        }
        
        // Check if username already exists
        if (userRepository.existsByUserName(normalizedUserName)) {
            result.put("success", false);
            result.put("message", "Username already taken");
            return result;
        }
        
        try {
            // Create new user
            User newUser = new User();
            newUser.setUserName(normalizedUserName);
            newUser.setEmail(normalizedEmail);
            newUser.setPasswordHash(passwordEncoder.encode(password));
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setGuest(false);
            newUser.setSegment(UserSegment.UNCLASSIFIED);
            newUser.setLastActivity(LocalDateTime.now());
            
            // Save user to database
            User savedUser = userRepository.save(newUser);
            
            // Prepare response data matching frontend expectations
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", savedUser.getId());
            userData.put("username", savedUser.getUserName());
            userData.put("email", savedUser.getEmail());
            userData.put("wallet", savedUser.getWalletBalance());
            userData.put("avatar", null); // Frontend expects avatar field
            
            result.put("success", true);
            result.put("message", "User registered successfully");
            result.put("data", userData);
            
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Registration failed: " + e.getMessage());
            e.printStackTrace(); // Log the exception for debugging
            return result;
        }
    }

    /**
     * Check if the given email belongs to an admin user
     * @param email The email to check
     * @return true if the email is the admin email, false otherwise
     */
    public boolean isAdmin(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedAdminEmail = adminEmail.trim().toLowerCase();
        return normalizedEmail.equals(normalizedAdminEmail);
    }
} 