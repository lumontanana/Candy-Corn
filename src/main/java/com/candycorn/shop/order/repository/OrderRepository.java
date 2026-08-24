package com.candycorn.shop.order.repository;

import com.candycorn.shop.order.entity.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndCustomerEmail(UUID id, String customerEmail);
}
