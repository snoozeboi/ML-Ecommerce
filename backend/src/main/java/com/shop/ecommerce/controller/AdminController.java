package com.shop.ecommerce.controller;

import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




@RestController
@RequestMapping("/admin") // Rest controller to access HTTP requests towards /admin, return vaules are automatically converted to JSON means no need for @ResponseBody
public class AdminController {


    // Injected repositories, means this controller has direct access to database through repositories. this controller does not need a service.

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/info")
    public Map<String, Object> getAdminInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // Get admin user
        User adminUser = userRepository.findByEmail("admin@ecommerce.com").orElse(null);
        if (adminUser != null) {
            Map<String, Object> admin = new HashMap<>();
            admin.put("id", adminUser.getId());
            admin.put("username", adminUser.getUserName());
            admin.put("email", adminUser.getEmail());
            admin.put("isGuest", adminUser.isGuest());
            admin.put("createdAt", adminUser.getCreatedAt());
            info.put("adminUser", admin);
        }
        
        // Get product count
        long productCount = productRepository.count();
        info.put("totalProducts", productCount);
        
        // Get sample products
        List<Product> products = productRepository.findAll();
        info.put("products", products);
        
        return info; // Returns all products and admin user.
    }

    @GetMapping("/users")
    public Map<String, Object> getAllUsers() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<User> users = userRepository.findAll();
            List<Map<String, Object>> userList = new ArrayList<>();
            
            for (User user : users) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("username", user.getUserName());
                userData.put("email", user.getEmail());
                userData.put("isGuest", user.isGuest());
                userData.put("segment", user.getSegment() != null ? user.getSegment().toString() : null);
                userData.put("createdAt", user.getCreatedAt());
                userData.put("lastActivity", user.getLastActivity());
                userData.put("eventCounter", user.getEventCounter());
                // Don't include password hash or lazy-loaded relationships
                userList.add(userData);
            }
            
            result.put("success", true);
            result.put("data", userList);
            result.put("count", userList.size());
            
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to retrieve users: " + e.getMessage());
            e.printStackTrace();
            return result;
        }
    }

    @GetMapping("/products")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
} 