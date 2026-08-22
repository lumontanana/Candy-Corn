package com.candycorn.shop.order.dto;

import java.util.UUID;

public record OrderItemRequest(UUID productId, int quantity) {
}
