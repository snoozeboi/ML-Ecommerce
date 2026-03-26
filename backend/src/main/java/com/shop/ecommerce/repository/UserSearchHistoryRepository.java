package com.shop.ecommerce.repository;

import com.shop.ecommerce.model.User;
import com.shop.ecommerce.model.UserSearchHistory;
import com.shop.ecommerce.model.UserSearchHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

public interface UserSearchHistoryRepository extends JpaRepository<UserSearchHistory, UserSearchHistoryId>{

  // get all search-terms for a user
  List<UserSearchHistory> findByUser(User user);

  // get all search terms for user id (avoids loading User)
  List<UserSearchHistory> findByIdUserId(int userId);

  // get one row by (userId, search-term)
  java.util.Optional<UserSearchHistory>
  findByIdUserIdAndIdSearchTerm(int userId, String searchTerm);

  // top 10 by count for userId
  List<UserSearchHistory> findTop10ByIdUserIdOrderBySearchCountDesc(int userId);



}
