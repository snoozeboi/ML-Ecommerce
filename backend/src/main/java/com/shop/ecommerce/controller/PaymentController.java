package com.shop.ecommerce.controller;

import com.shop.ecommerce.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Create a payment intent for checkout
     * POST /api/payments/create-intent
     * Body: { userId: int, amount: float, currency: string (optional, default: "usd") }
     */
    @PostMapping("/create-intent")
    public Map<String, Object> createPaymentIntent(@RequestBody Map<String, Object> request) {
        int userId = (Integer) request.get("userId");
        double amountDouble = ((Number) request.get("amount")).doubleValue();
        float amount = (float) amountDouble;
        String currency = request.containsKey("currency") ?
                (String) request.get("currency") : "usd";
        String type = request.containsKey("type") ? (String) request.get("type") : "checkout";

        return paymentService.createPaymentIntent(userId, amount, currency, type);
    }

    /**
     * Create a payment intent for guest checkout (no user required)
     * POST /api/payments/create-guest-intent
     * Body: { amount: float, currency: string (optional, default: "usd"), productIds: int[] (optional) }
     */
    @PostMapping("/create-guest-intent")
    public Map<String, Object> createGuestPaymentIntent(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (request == null || !request.containsKey("amount")) {
                response.put("success", false);
                response.put("message", "Amount is required");
                return response;
            }
            
            Object amountObj = request.get("amount");
            if (amountObj == null) {
                response.put("success", false);
                response.put("message", "Amount cannot be null");
                return response;
            }
            
            double amountDouble;
            if (amountObj instanceof Number) {
                amountDouble = ((Number) amountObj).doubleValue();
            } else if (amountObj instanceof String) {
                try {
                    amountDouble = Double.parseDouble((String) amountObj);
                } catch (NumberFormatException e) {
                    response.put("success", false);
                    response.put("message", "Invalid amount format: " + amountObj);
                    return response;
                }
            } else {
                response.put("success", false);
                response.put("message", "Amount must be a number");
                return response;
            }
            
            float amount = (float) amountDouble;
            String currency = request.containsKey("currency") && request.get("currency") != null ?
                    (String) request.get("currency") : "usd";
            
            // Extract product IDs if provided (for interaction tracking)
            List<Integer> productIds = null;
            if (request.containsKey("productIds") && request.get("productIds") != null) {
                Object productIdsObj = request.get("productIds");
                if (productIdsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> productIdsList = (List<Object>) productIdsObj;
                    productIds = new ArrayList<>();
                    for (Object id : productIdsList) {
                        if (id instanceof Number) {
                            productIds.add(((Number) id).intValue());
                        }
                    }
                }
            }

            return paymentService.createGuestPaymentIntent(amount, currency, productIds);
        } catch (Exception e) {
            System.err.println("Error in createGuestPaymentIntent controller: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error processing request: " + e.getMessage());
            return response;
        }
    }

    /**
     * Pay with wallet balance. Body: { userId: int, amount: number }
     */
    @PostMapping("/pay-with-wallet")
    public Map<String, Object> payWithWallet(@RequestBody Map<String, Object> request) {
        int userId = (Integer) request.get("userId");
        double amount = ((Number) request.get("amount")).doubleValue();
        return paymentService.payWithWallet(userId, amount);
    }

    /**
     * Confirm a payment
     * POST /api/payments/confirm
     * Body: { paymentIntentId: string, paymentMethodId: string (optional) }
     */
    @PostMapping("/confirm")
    public Map<String, Object> confirmPayment(@RequestBody Map<String, String> request) {
        String paymentIntentId = request.get("paymentIntentId");
        String paymentMethodId = request.get("paymentMethodId");
        
        return paymentService.confirmPayment(paymentIntentId, paymentMethodId);
    }

    /**
     * Save a payment method for a user
     * POST /api/payments/save-method
     * Body: { userId: int, stripePaymentMethodId: string }
     */
    @PostMapping("/save-method")
    public Map<String, Object> savePaymentMethod(@RequestBody Map<String, Object> request) {
        int userId = (Integer) request.get("userId");
        String stripePaymentMethodId = (String) request.get("stripePaymentMethodId");
        
        return paymentService.savePaymentMethod(userId, stripePaymentMethodId);
    }

    /**
     * Get all payment methods for a user
     * GET /api/payments/methods/{userId}
     */
    @GetMapping("/methods/{userId}")
    public Map<String, Object> getUserPaymentMethods(@PathVariable int userId) {
        return paymentService.getUserPaymentMethods(userId);
    }

    /**
     * Set default payment method
     * PUT /api/payments/methods/{userId}/default
     * Body: { paymentMethodId: int }
     */
    @PutMapping("/methods/{userId}/default")
    public Map<String, Object> setDefaultPaymentMethod(
            @PathVariable int userId,
            @RequestBody Map<String, Integer> request) {
        int paymentMethodId = request.get("paymentMethodId");
        
        return paymentService.setDefaultPaymentMethod(userId, paymentMethodId);
    }

    /**
     * Delete a payment method
     * DELETE /api/payments/methods/{userId}/{paymentMethodId}
     */
    @DeleteMapping("/methods/{userId}/{paymentMethodId}")
    public Map<String, Object> deletePaymentMethod(
            @PathVariable int userId,
            @PathVariable int paymentMethodId) {
        
        return paymentService.deletePaymentMethod(userId, paymentMethodId);
    }

    /**
     * Get payment history for a user
     * GET /api/payments/history/{userId}
     */
    @GetMapping("/history/{userId}")
    public Map<String, Object> getPaymentHistory(@PathVariable int userId) {
        return paymentService.getPaymentHistory(userId);
    }
}
