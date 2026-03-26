package com.shop.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_search_history")
public class UserSearchHistory {

  @Getter
  @EmbeddedId
  private UserSearchHistoryId id;

  @JsonIgnore
  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private User user;

  @Setter
  @Getter
  @Column(name = "search_count")
  private int searchCount;

  @Setter
  @Getter
  @Column(name = "last_searched_at")
  private LocalDateTime lastSearchedAt;

  public UserSearchHistory() {}

  public UserSearchHistory(User user, String searchTerm) {
    this.user = user;
    this.id = new UserSearchHistoryId(user.getId(), searchTerm);
    this.searchCount = 1;
    this.lastSearchedAt = LocalDateTime.now();
  }


}