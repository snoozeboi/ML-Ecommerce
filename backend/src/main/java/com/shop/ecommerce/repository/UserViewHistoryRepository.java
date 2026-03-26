package com.shop.ecommerce.repository;

import com.shop.ecommerce.model.UserViewHistory;
import com.shop.ecommerce.model.UserViewHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserViewHistoryRepository extends JpaRepository<UserViewHistory, UserViewHistoryId> {
}
