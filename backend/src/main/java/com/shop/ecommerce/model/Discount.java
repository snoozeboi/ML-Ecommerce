package com.shop.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/// JPA entity representing a percentage-based discount for a product.
/// Used by ProductService to determine if a discount is active and how much to apply.

@Entity
@Table(name = "discount_list")
public class Discount {

  @Getter
  @Setter
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  // 0–100 (e.g. 20 = 20% off)
  @Column(nullable = false)
  @Getter @Setter
  private int percent;

  @Getter
  @Setter
  @Column(name = "discount_start")
  private LocalDateTime startsAt;

  @Getter
  @Setter
  @Column(name = "discount_end")
  private LocalDateTime endsAt;

  @Getter
  @Setter
  @Column(nullable = false)
  private boolean active = true;

  @Getter
  @Setter
  @JsonIgnore
  @OneToOne(mappedBy = "discount")
  private Product product;

  public Discount() {}

  public boolean isActiveNow(LocalDateTime now) {
    if (!active) return false;
    if (startsAt != null && now.isBefore(startsAt)) return false;
    if (endsAt != null && now.isAfter(endsAt)) return false;
    return true;
  }

}
