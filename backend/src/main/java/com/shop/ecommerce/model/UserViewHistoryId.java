package com.shop.ecommerce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserViewHistoryId implements Serializable {

  @Column(name = "user_id")
  private int userId;

  @Column(name = "product_id")
  private int productId;

  public UserViewHistoryId() {}

  public UserViewHistoryId(int userId, int productId) {
    this.userId = userId;
    this.productId = productId;
  }

  public int getUserId() { return userId; }
  public int getProductId() { return productId; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserViewHistoryId that = (UserViewHistoryId) o;
    return userId == that.userId && productId == that.productId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId,productId);
  }

}
