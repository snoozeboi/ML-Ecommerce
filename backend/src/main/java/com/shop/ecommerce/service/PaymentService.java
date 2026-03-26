package com.shop.ecommerce.service;

import com.shop.ecommerce.model.CartItem;
import com.shop.ecommerce.model.Payment;
import com.shop.ecommerce.model.PaymentMethod;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.controller.RecommendationController;
import com.shop.ecommerce.repository.CartItemRepository;
import com.shop.ecommerce.repository.PaymentRepository;
import com.shop.ecommerce.repository.PaymentMethodRepository;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentMethodAttachParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Add cart items to user's purchase history and clear the cart (used after successful checkout).
     * Also immediately updates ML service with purchase interactions.
     */
    private void completeCheckoutForUser(int userId) {
        User u = userRepository.findById(userId).orElse(null);
        if (u == null) return;
        List<CartItem> items = cartItemRepository.findByUser(u);
        if (items.isEmpty()) return;
        List<Integer> ph = u.getPurchaseHistory() != null ? new ArrayList<>(u.getPurchaseHistory()) : new ArrayList<>();
        
        // Track purchased product IDs for ML service update
        List<Integer> purchasedProductIds = new ArrayList<>();
        
        for (CartItem item : items) {
            int productId = item.getProduct().getId();
            int qty = item.getQuantity();
            for (int i = 0; i < qty; i++) {
                ph.add(productId);
                purchasedProductIds.add(productId);
            }
        }
        u.setPurchaseHistory(ph);
        userRepository.save(u);
        cartItemRepository.deleteAll(items);
        
        // ✅ Immediately update ML service with purchase interactions
        if (recommendationController != null) {
            try {
                // Send purchase interactions to ML service for immediate update
                for (Integer productId : purchasedProductIds) {
                    recommendationController.recordPurchaseInteraction(userId, productId);
                }
                
                // Force reload of all data to ML service (bypass TTL cache)
                recommendationController.forceReloadMLData();
            } catch (Exception e) {
                System.err.println("Error updating ML service after purchase: " + e.getMessage());
                // Don't fail the checkout if ML update fails
            }
        }
    }

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RecommendationController recommendationController;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Autowired
    public PaymentService(@Value("${stripe.secret.key}") String stripeSecretKey) {
        this.stripeSecretKey = stripeSecretKey;
        Stripe.apiKey = stripeSecretKey;
    }

    /**
     * Get or create Stripe Customer for the user. Saved payment methods must be attached to a Customer to be reused.
     */
    private String getOrCreateStripeCustomer(User user) throws StripeException {
        if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isBlank()) {
            return user.getStripeCustomerId();
        }
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail() != null ? user.getEmail() : null)
                .putMetadata("user_id", String.valueOf(user.getId()))
                .build();
        Customer customer = Customer.create(params);
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);
        return customer.getId();
    }

    /**
     * Create a payment intent for checkout or top-up
     * @param type "checkout" or "top_up"
     */
    public Map<String, Object> createPaymentIntent(int userId, float amount, String currency, String type) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (amount <= 0) {
                response.put("success", false);
                response.put("message", "Amount must be greater than 0");
                return response;
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            User user = userOpt.get();
            String stripeCustomerId = getOrCreateStripeCustomer(user);

            // Convert amount to cents (Stripe uses smallest currency unit)
            long amountInCents = Math.round(amount * 100);
            if (amountInCents <= 0) {
                response.put("success", false);
                response.put("message", "Amount must be greater than 0");
                return response;
            }

            // Create PaymentIntent with customer so saved payment methods (attached to this customer) can be used
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setCustomer(stripeCustomerId)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putMetadata("user_id", String.valueOf(userId))
                    .putMetadata("type", type != null ? type : "checkout")
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Save payment record
            Payment payment = new Payment(user, amount, currency, paymentIntent.getId());
            payment.setDescription("top_up".equals(type) ? "Wallet top-up" : "Payment for order");
            payment = paymentRepository.save(payment);

            response.put("success", true);
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentIntentId", paymentIntent.getId());
            response.put("paymentId", payment.getId());
            
        } catch (StripeException e) {
            response.put("success", false);
            response.put("message", "Failed to create payment intent: " + e.getMessage());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Create a payment intent for guest checkout (no user required)
     * @param amount Payment amount
     * @param currency Currency code (default: "usd")
     * @param productIds Optional list of product IDs for interaction tracking
     * @return Payment intent response
     */
    public Map<String, Object> createGuestPaymentIntent(float amount, String currency, List<Integer> productIds) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (amount <= 0) {
                response.put("success", false);
                response.put("message", "Amount must be greater than 0");
                return response;
            }

            // Convert amount to cents (Stripe uses smallest currency unit)
            long amountInCents = Math.round(amount * 100);
            if (amountInCents <= 0) {
                response.put("success", false);
                response.put("message", "Amount must be greater than 0");
                return response;
            }

            // Create PaymentIntent without customer (guest checkout)
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency != null ? currency.toLowerCase() : "usd")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putMetadata("type", "guest_checkout")
                    .putMetadata("is_guest", "true");
            
            // Add product IDs to metadata for interaction tracking
            if (productIds != null && !productIds.isEmpty()) {
                paramsBuilder.putMetadata("product_ids", productIds.toString());
            }
            
            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());

            // Save payment record without user (guest payment)
            Payment payment = new Payment(amount, currency != null ? currency : "usd", paymentIntent.getId());
            payment.setDescription("Guest checkout payment");
            payment = paymentRepository.save(payment);

            response.put("success", true);
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentIntentId", paymentIntent.getId());
            response.put("paymentId", payment.getId());
            
        } catch (StripeException e) {
            System.err.println("Stripe error creating guest payment intent: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Failed to create payment intent: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error creating guest payment intent: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Confirm a payment intent (or sync status if already confirmed by frontend).
     * Response contains only serializable fields (no Stripe objects) to avoid JSON errors.
     */
    @Transactional
    public Map<String, Object> confirmPayment(String paymentIntentId, String paymentMethodId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (paymentIntentId == null || paymentIntentId.isBlank()) {
                response.put("success", false);
                response.put("message", "Payment intent ID is required");
                return response;
            }

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            String status = paymentIntent.getStatus();

            // Only confirm if not already succeeded (frontend may have already confirmed with saved card)
            if (!"succeeded".equals(status) && !"processing".equals(status)) {
                if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
                    PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                            .setPaymentMethod(paymentMethodId)
                            .build();
                    paymentIntent = paymentIntent.confirm(params);
                } else {
                    paymentIntent = paymentIntent.confirm();
                }
                status = paymentIntent.getStatus();
            }

            // Update payment status in database
            Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntentId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                status = paymentIntent.getStatus();

                if ("succeeded".equals(status)) {
                    payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                    Object typeObj = paymentIntent.getMetadata() != null ? paymentIntent.getMetadata().get("type") : null;
                    Object isGuestObj = paymentIntent.getMetadata() != null ? paymentIntent.getMetadata().get("is_guest") : null;
                    
                    // Only update user-specific data if this is not a guest payment
                    if (!"true".equals(isGuestObj) && payment.getUser() != null) {
                        int userId = payment.getUser().getId();
                        if ("top_up".equals(typeObj)) {
                            User u = userRepository.findById(userId).orElse(null);
                            if (u != null) {
                                u.setWalletBalance(u.getWalletBalance() + payment.getAmount());
                                userRepository.save(u);
                            }
                        } else if ("checkout".equals(typeObj)) {
                            completeCheckoutForUser(userId);
                            if (recommendationController != null) {
                                recommendationController.invalidateMLDataCache();
                            }
                        }
                    } else if ("true".equals(isGuestObj)) {
                        // For guest payments, track product interactions without user
                        // Extract product IDs from metadata and record interactions
                        Object productIdsObj = paymentIntent.getMetadata() != null ? paymentIntent.getMetadata().get("product_ids") : null;
                        if (productIdsObj != null) {
                            try {
                                String productIdsStr = productIdsObj.toString();
                                // Parse product IDs from string like "[1, 2, 3]"
                                productIdsStr = productIdsStr.replaceAll("[\\[\\]\\s]", "");
                                String[] productIdArray = productIdsStr.split(",");
                                for (String productIdStr : productIdArray) {
                                    if (!productIdStr.isEmpty()) {
                                        int productId = Integer.parseInt(productIdStr.trim());
                                        // Record guest purchase interaction (product-level only, no user)
                                        recordGuestProductInteraction(productId);
                                    }
                                }
                            } catch (Exception e) {
                                // Log but don't fail payment if interaction tracking fails
                                System.err.println("Failed to record guest product interactions: " + e.getMessage());
                            }
                        }
                    }
                } else if ("processing".equals(status)) {
                    payment.setStatus(Payment.PaymentStatus.PROCESSING);
                } else if ("requires_payment_method".equals(status) ||
                        "requires_action".equals(status)) {
                    payment.setStatus(Payment.PaymentStatus.PENDING);
                } else {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                }

                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }

            response.put("success", "succeeded".equals(paymentIntent.getStatus()));
            response.put("status", paymentIntent.getStatus());
            // Do not put Stripe PaymentIntent object - it does not serialize to JSON reliably
            if (response.containsKey("message") == false && !Boolean.TRUE.equals(response.get("success"))) {
                response.put("message", "Payment status: " + paymentIntent.getStatus());
            }
        } catch (StripeException e) {
            response.put("success", false);
            response.put("message", "Payment failed: " + e.getMessage());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Save a payment method for a user
     */
    public Map<String, Object> savePaymentMethod(int userId, String stripePaymentMethodId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            User user = userOpt.get();
            String stripeCustomerId = getOrCreateStripeCustomer(user);

            // Retrieve payment method from Stripe and attach to customer (required for reuse)
            com.stripe.model.PaymentMethod stripePaymentMethod = com.stripe.model.PaymentMethod.retrieve(stripePaymentMethodId);
            stripePaymentMethod.attach(
                    PaymentMethodAttachParams.builder().setCustomer(stripeCustomerId).build()
            );

            // Check if already exists in our DB
            Optional<PaymentMethod> existingOpt = paymentMethodRepository
                    .findByStripePaymentMethodId(stripePaymentMethodId);

            if (existingOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Payment method already saved");
                return response;
            }

            // Create new payment method record
            PaymentMethod paymentMethod = new PaymentMethod();
            paymentMethod.setUser(user);
            paymentMethod.setStripePaymentMethodId(stripePaymentMethodId);
            paymentMethod.setType(stripePaymentMethod.getType());

            // Extract card details if available
            if (stripePaymentMethod.getCard() != null) {
                paymentMethod.setLast4(stripePaymentMethod.getCard().getLast4());
                paymentMethod.setBrand(stripePaymentMethod.getCard().getBrand());
                Long expMonth = stripePaymentMethod.getCard().getExpMonth();
                Long expYear = stripePaymentMethod.getCard().getExpYear();
                paymentMethod.setExpMonth(expMonth != null ? expMonth.intValue() : null);
                paymentMethod.setExpYear(expYear != null ? expYear.intValue() : null);
            }

            // If this is the first payment method, set as default
            List<PaymentMethod> existingMethods = paymentMethodRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (existingMethods.isEmpty()) {
                paymentMethod.setDefault(true);
            }

            paymentMethod = paymentMethodRepository.save(paymentMethod);

            response.put("success", true);
            response.put("paymentMethod", paymentMethod);
            
        } catch (StripeException e) {
            response.put("success", false);
            response.put("message", "Failed to save payment method: " + e.getMessage());
            e.printStackTrace(); // Log for debugging
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error saving payment method: " + e.getMessage());
            e.printStackTrace(); // Log for debugging
        }

        return response;
    }

    /**
     * Get all payment methods for a user
     */
    public Map<String, Object> getUserPaymentMethods(int userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<PaymentMethod> paymentMethods = paymentMethodRepository
                    .findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            
            List<Map<String, Object>> methodsList = new ArrayList<>();
            for (PaymentMethod pm : paymentMethods) {
                Map<String, Object> methodData = new HashMap<>();
                methodData.put("id", pm.getId());
                methodData.put("stripePaymentMethodId", pm.getStripePaymentMethodId());
                methodData.put("type", pm.getType());
                methodData.put("last4", pm.getLast4());
                methodData.put("brand", pm.getBrand());
                methodData.put("expMonth", pm.getExpMonth());
                methodData.put("expYear", pm.getExpYear());
                methodData.put("isDefault", pm.isDefault());
                methodsList.add(methodData);
            }

            response.put("success", true);
            response.put("paymentMethods", methodsList);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Set default payment method
     */
    public Map<String, Object> setDefaultPaymentMethod(int userId, int paymentMethodId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<PaymentMethod> paymentMethodOpt = paymentMethodRepository.findById(paymentMethodId);
            if (paymentMethodOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Payment method not found");
                return response;
            }

            PaymentMethod paymentMethod = paymentMethodOpt.get();
            
            // Verify it belongs to the user
            if (paymentMethod.getUser().getId() != userId) {
                response.put("success", false);
                response.put("message", "Unauthorized");
                return response;
            }

            // Unset current default
            Optional<PaymentMethod> currentDefaultOpt = paymentMethodRepository
                    .findByUserIdAndIsDefaultTrue(userId);
            if (currentDefaultOpt.isPresent()) {
                PaymentMethod currentDefault = currentDefaultOpt.get();
                currentDefault.setDefault(false);
                paymentMethodRepository.save(currentDefault);
            }

            // Set new default
            paymentMethod.setDefault(true);
            paymentMethod.setUpdatedAt(LocalDateTime.now());
            paymentMethodRepository.save(paymentMethod);

            response.put("success", true);
            response.put("message", "Default payment method updated");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Delete a payment method
     * Note: stripe_customer_id in users table is NOT removed - it's needed for future payment methods
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deletePaymentMethod(int userId, int paymentMethodId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("DEBUG: Attempting to delete payment method ID: " + paymentMethodId + " for user ID: " + userId);
            
            Optional<PaymentMethod> paymentMethodOpt = paymentMethodRepository.findById(paymentMethodId);
            if (paymentMethodOpt.isEmpty()) {
                System.out.println("DEBUG: Payment method not found in database");
                response.put("success", false);
                response.put("message", "Payment method not found");
                return response;
            }

            PaymentMethod paymentMethod = paymentMethodOpt.get();
            System.out.println("DEBUG: Found payment method - User ID: " + paymentMethod.getUser().getId() + ", Stripe ID: " + paymentMethod.getStripePaymentMethodId());
            
            // Verify it belongs to the user
            if (paymentMethod.getUser().getId() != userId) {
                System.out.println("DEBUG: Unauthorized - payment method belongs to different user");
                response.put("success", false);
                response.put("message", "Unauthorized");
                return response;
            }

            String stripePaymentMethodId = paymentMethod.getStripePaymentMethodId();
            
            // Delete from Stripe first (but don't fail if this fails)
            try {
                System.out.println("DEBUG: Attempting to detach from Stripe: " + stripePaymentMethodId);
                com.stripe.model.PaymentMethod stripePM = com.stripe.model.PaymentMethod.retrieve(stripePaymentMethodId);
                stripePM.detach();
                System.out.println("DEBUG: Successfully detached from Stripe");
            } catch (StripeException e) {
                // Log but continue with database deletion
                System.err.println("WARNING: Failed to delete from Stripe: " + e.getMessage());
                System.err.println("Continuing with database deletion...");
            }

            // Delete from database - try multiple approaches
            System.out.println("DEBUG: Deleting from database...");
            
            // First, try using deleteById
            try {
                paymentMethodRepository.deleteById(paymentMethodId);
                paymentMethodRepository.flush();
                System.out.println("DEBUG: DeleteById completed");
            } catch (Exception e) {
                System.err.println("WARNING: deleteById failed, trying delete with entity: " + e.getMessage());
                // Fallback to delete with entity
                paymentMethodRepository.delete(paymentMethod);
                paymentMethodRepository.flush();
                System.out.println("DEBUG: Delete with entity completed");
            }
            
            // If that doesn't work, try JPQL query
            Optional<PaymentMethod> verifyDelete = paymentMethodRepository.findById(paymentMethodId);
            if (verifyDelete.isPresent()) {
                System.err.println("WARNING: Payment method still exists after standard delete, trying JPQL...");
                try {
                    int deletedCount = entityManager.createQuery(
                        "DELETE FROM PaymentMethod pm WHERE pm.id = :id")
                        .setParameter("id", paymentMethodId)
                        .executeUpdate();
                    entityManager.flush();
                    System.out.println("DEBUG: JPQL query deleted " + deletedCount + " record(s)");
                } catch (Exception e2) {
                    System.err.println("ERROR: JPQL deletion failed: " + e2.getMessage());
                    e2.printStackTrace();
                }
            }
            
            // Final verification
            verifyDelete = paymentMethodRepository.findById(paymentMethodId);
            if (verifyDelete.isPresent()) {
                System.err.println("ERROR: Payment method STILL exists after all deletion attempts!");
                System.err.println("This may indicate a database constraint or transaction issue.");
                response.put("success", false);
                response.put("message", "Payment method deletion failed - record still exists in database. Check server logs for details.");
                return response;
            }
            
            System.out.println("DEBUG: Verification passed - payment method successfully deleted from PostgreSQL");

            response.put("success", true);
            response.put("message", "Payment method deleted successfully");
            
        } catch (Exception e) {
            System.err.println("ERROR: Exception during payment method deletion:");
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Pay with wallet balance (deduct from wallet, clear cart)
     */
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> payWithWallet(int userId, double amount) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (amount <= 0) {
                response.put("success", false);
                response.put("message", "Amount must be greater than 0");
                return response;
            }
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }
            User user = userOpt.get();
            if (user.getWalletBalance() < amount) {
                response.put("success", false);
                response.put("message", "Insufficient wallet balance. Add funds first.");
                return response;
            }
            user.setWalletBalance(user.getWalletBalance() - amount);
            userRepository.save(user);
            List<CartItem> items = cartItemRepository.findByUser(user);
            List<Integer> ph = user.getPurchaseHistory() != null ? new ArrayList<>(user.getPurchaseHistory()) : new ArrayList<>();
            
            // Track purchased product IDs for ML service update
            List<Integer> purchasedProductIds = new ArrayList<>();
            
            for (CartItem item : items) {
                int productId = item.getProduct().getId();
                for (int i = 0; i < item.getQuantity(); i++) {
                    ph.add(productId);
                    purchasedProductIds.add(productId);
                }
            }
            user.setPurchaseHistory(ph);
            userRepository.save(user);
            cartItemRepository.deleteAll(items);
            Payment payment = new Payment(user, (float) amount, "usd", "wallet_" + userId + "_" + System.currentTimeMillis());
            payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
            payment.setDescription("Checkout (wallet)");
            paymentRepository.save(payment);
            
            // ✅ Immediately update ML service with purchase interactions
            if (recommendationController != null) {
                try {
                    // Send purchase interactions to ML service for immediate update
                    for (Integer productId : purchasedProductIds) {
                        recommendationController.recordPurchaseInteraction(userId, productId);
                    }
                    
                    // Force reload of all data to ML service (bypass TTL cache)
                    recommendationController.forceReloadMLData();
                } catch (Exception e) {
                    System.err.println("Error updating ML service after wallet payment: " + e.getMessage());
                    // Don't fail the payment if ML update fails
                }
            }
            response.put("success", true);
            response.put("message", "Payment successful");
            response.put("newBalance", user.getWalletBalance());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        return response;
    }

    /**
     * Get payment history for a user
     */
    public Map<String, Object> getPaymentHistory(int userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            List<Map<String, Object>> paymentsList = new ArrayList<>();
            for (Payment payment : payments) {
                Map<String, Object> paymentData = new HashMap<>();
                paymentData.put("id", payment.getId());
                paymentData.put("amount", payment.getAmount());
                paymentData.put("currency", payment.getCurrency());
                paymentData.put("status", payment.getStatus().toString());
                paymentData.put("createdAt", payment.getCreatedAt());
                paymentData.put("description", payment.getDescription());
                paymentsList.add(paymentData);
            }

            response.put("success", true);
            response.put("payments", paymentsList);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Record product interaction for guest purchases (product-level tracking only, no user).
     * Increments product views as a proxy for purchase/popularity for analytics.
     */
    private void recordGuestProductInteraction(int productId) {
        productRepository.findById(productId).ifPresent(p -> {
            p.setViews(p.getViews() + 1);
            productRepository.save(p);
        });
    }
}
