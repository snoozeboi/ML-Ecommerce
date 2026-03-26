package com.shop.ecommerce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserSearchHistoryId implements Serializable {

  @Column(name = "user_id")
  private int userId;

  @Column(name = "search_term")
  private String searchTerm;

  public int getUserId() { return userId; }
  public String getSearchTerm() { return searchTerm; }

  public UserSearchHistoryId() {}

  public UserSearchHistoryId(int userId, String searchTerm) {
    this.userId = userId;
    this.searchTerm = searchTerm;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserSearchHistoryId that = (UserSearchHistoryId) o;
    return userId == that.userId &&
            Objects.equals(searchTerm, that.searchTerm);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, searchTerm);
  }
}