package com.candycorn.shop.order.dto;

import com.candycorn.shop.order.entity.Address;

public record AddressResponse(
        String recipientName,
        String street,
        String city,
        String postalCode,
        String country,
        String phone) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getRecipientName(),
                address.getStreet(),
                address.getCity(),
                address.getPostalCode(),
                address.getCountry(),
                address.getPhone());
    }
}
