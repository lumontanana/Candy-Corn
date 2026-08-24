package com.candycorn.shop.order.dto;

import java.util.List;

public record CreateOrderRequest(
        String customerName,
        String customerEmail,
        AddressRequest shippingAddress,
        List<OrderItemRequest> items) {
}
