package com.shop.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "user_view_history")
public class UserViewHistory {

  @Getter
  @EmbeddedId
  private UserViewHistoryId id;

  @Getter
  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private User user;

  @Setter
  @Getter
  @Column(name = "view_count")
  private int viewCount;

  @Setter
  @Getter
  @Column(name = "last_viewed_at")
  private LocalDateTime lastViewedAt;

  public UserViewHistory() {}

  public UserViewHistory(User user, int productId) {
    this.user = user;
    this.id = new UserViewHistoryId(user.getId(),productId);
    this.viewCount = 0;
    this.lastViewedAt = LocalDateTime.now();
  }


}
