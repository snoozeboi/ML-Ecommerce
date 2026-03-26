package com.shop.ecommerce.repository;

import com.shop.ecommerce.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {
    Optional<PaymentMethod> findByStripePaymentMethodId(String stripePaymentMethodId);
    List<PaymentMethod> findByUserIdOrderByIsDefaultDescCreatedAtDesc(int userId);
    Optional<PaymentMethod> findByUserIdAndIsDefaultTrue(int userId);
}
