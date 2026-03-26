package com.shop.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

    @Getter
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Getter
    @Setter
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Getter
    @Setter
    @Column(name = "stripe_payment_method_id", unique = true, nullable = false)
    private String stripePaymentMethodId;

    @Getter
    @Setter
    @Column(name = "stripe_customer_id")
    private String stripeCustomerId; // Optional - Stripe customer ID if needed

    @Getter
    @Setter
    @Column(name = "type", nullable = false)
    private String type; // card, etc.

    @Getter
    @Setter
    @Column(name = "last4")
    private String last4; // Last 4 digits of card

    @Getter
    @Setter
    @Column(name = "brand")
    private String brand; // visa, mastercard, etc.

    @Getter
    @Setter
    @Column(name = "exp_month")
    private Integer expMonth;

    @Getter
    @Setter
    @Column(name = "exp_year")
    private Integer expYear;

    @Getter
    @Setter
    @Column(name = "is_default")
    private boolean isDefault = false;

    @Getter
    @Setter
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Default constructor
    public PaymentMethod() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public PaymentMethod(User user, String stripePaymentMethodId, String type) {
        this.user = user;
        this.stripePaymentMethodId = stripePaymentMethodId;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
