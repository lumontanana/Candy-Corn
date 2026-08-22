package com.candycorn.shop.order.dto;

import com.candycorn.shop.order.entity.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerName,
        String customerEmail,
        String status,
        AddressResponse shippingAddress,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        Instant createdAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus().name(),
                AddressResponse.from(order.getShippingAddress()),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }
}
