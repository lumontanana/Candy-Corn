package com.candycorn.shop.order.service;

import com.candycorn.shop.catalog.entity.Product;
import com.candycorn.shop.catalog.repository.ProductRepository;
import com.candycorn.shop.common.exception.InvalidRequestException;
import com.candycorn.shop.common.exception.ResourceNotFoundException;
import com.candycorn.shop.order.dto.AddressRequest;
import com.candycorn.shop.order.dto.CreateOrderRequest;
import com.candycorn.shop.order.dto.OrderItemRequest;
import com.candycorn.shop.order.dto.OrderResponse;
import com.candycorn.shop.order.entity.Address;
import com.candycorn.shop.order.entity.Order;
import com.candycorn.shop.order.repository.OrderRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        validateCustomer(request);
        validateAddress(request.shippingAddress());
        if (request.items() == null || request.items().isEmpty()) {
            throw new InvalidRequestException("An order must contain at least one item");
        }

        Address shippingAddress = new Address(
                request.shippingAddress().recipientName(),
                request.shippingAddress().street(),
                request.shippingAddress().city(),
                request.shippingAddress().postalCode(),
                request.shippingAddress().country(),
                request.shippingAddress().phone());
        Order order = new Order(request.customerName(), request.customerEmail(), shippingAddress);

        for (OrderItemRequest itemRequest : request.items()) {
            addItem(order, itemRequest);
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    private void addItem(Order order, OrderItemRequest itemRequest) {
        if (itemRequest.quantity() < 1) {
            throw new InvalidRequestException("quantity must be greater than 0");
        }
        Product product = productRepository.findByIdAndActiveTrue(itemRequest.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.productId()));
        if (product.getStock() < itemRequest.quantity()) {
            throw new InvalidRequestException("Insufficient stock for product: " + product.getName());
        }
        product.changeStock(product.getStock() - itemRequest.quantity());
        order.addItem(product, itemRequest.quantity());
    }

    private void validateCustomer(CreateOrderRequest request) {
        if (request.customerName() == null || request.customerName().isBlank()) {
            throw new InvalidRequestException("customerName is required");
        }
        if (request.customerEmail() == null || request.customerEmail().isBlank()) {
            throw new InvalidRequestException("customerEmail is required");
        }
    }

    private void validateAddress(AddressRequest address) {
        if (address == null) {
            throw new InvalidRequestException("shippingAddress is required");
        }
        if (address.recipientName() == null || address.recipientName().isBlank()) {
            throw new InvalidRequestException("shippingAddress.recipientName is required");
        }
        if (address.street() == null || address.street().isBlank()) {
            throw new InvalidRequestException("shippingAddress.street is required");
        }
        if (address.city() == null || address.city().isBlank()) {
            throw new InvalidRequestException("shippingAddress.city is required");
        }
        if (address.postalCode() == null || address.postalCode().isBlank()) {
            throw new InvalidRequestException("shippingAddress.postalCode is required");
        }
        if (address.country() == null || address.country().isBlank()) {
            throw new InvalidRequestException("shippingAddress.country is required");
        }
    }

    public OrderResponse findByIdForCustomer(UUID id, String customerEmail) {
        return orderRepository.findByIdAndCustomerEmail(id, customerEmail)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }
}
