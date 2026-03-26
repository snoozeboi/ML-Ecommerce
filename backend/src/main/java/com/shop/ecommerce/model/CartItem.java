package com.shop.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;       // for date and time
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "cart_items")
public class CartItem {

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Getter
    @Setter
    @Column(nullable = false)
    private int quantity;

    @Getter
    @Setter
    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @Getter
    @Setter
    @Column(name = "price_at_add", nullable = false)
    private float priceAtAdd;

    @Getter
    @Setter
    @ElementCollection
    @CollectionTable(name = "cart_item_options", joinColumns = @JoinColumn(name = "cart_item_id"))
    @MapKeyColumn(name = "option_name")
    @Column(name = "option_value")
    private Map<String, String> options; // left-side: the option's name, right-side: the option's detail

    // Default constructor required by JPA
    public CartItem() {
        this.options = new HashMap<>();
    }

    public CartItem(User user, Product product, int quantity, float priceAtAdd, Map<String, String> options) {
        this.user = user;
        this.product = product;
        this.quantity = quantity;
        this.addedAt = LocalDateTime.now();
        this.priceAtAdd = priceAtAdd;
        this.options = options != null ? options : new HashMap<>();
    }

}