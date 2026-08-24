package com.candycorn.shop.order.dto;

public record AddressRequest(
        String recipientName,
        String street,
        String city,
        String postalCode,
        String country,
        String phone) {
}
