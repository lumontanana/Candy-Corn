package com.candycorn.shop.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.candycorn.shop.catalog.entity.Category;
import com.candycorn.shop.catalog.entity.Product;
import com.candycorn.shop.catalog.repository.ProductRepository;
import com.candycorn.shop.common.exception.InvalidRequestException;
import com.candycorn.shop.common.exception.ResourceNotFoundException;
import com.candycorn.shop.order.dto.AddressRequest;
import com.candycorn.shop.order.dto.CreateOrderRequest;
import com.candycorn.shop.order.dto.OrderItemRequest;
import com.candycorn.shop.order.dto.OrderResponse;
import com.candycorn.shop.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final OrderService orderService = new OrderService(orderRepository, productRepository);

    private final Category category = new Category("Gominolas", "gominolas");
    private final AddressRequest address = new AddressRequest(
            "Ana", "Calle Falsa 123", "Madrid", "28080", "ES", "600000000");

    private CreateOrderRequest requestWithItems(List<OrderItemRequest> items) {
        return new CreateOrderRequest("Ana", "ana@example.com", address, items);
    }

    @Test
    void rejectsBlankCustomerName() {
        CreateOrderRequest request = new CreateOrderRequest("", "ana@example.com", address, List.of());
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("customerName is required");
    }

    @Test
    void rejectsIncompleteAddress() {
        AddressRequest incomplete = new AddressRequest("Ana", "", "Madrid", "28080", "ES", null);
        CreateOrderRequest request = new CreateOrderRequest("Ana", "ana@example.com", incomplete, List.of());
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("shippingAddress.street is required");
    }

    @Test
    void rejectsEmptyItemList() {
        assertThatThrownBy(() -> orderService.createOrder(requestWithItems(List.of())))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("An order must contain at least one item");
    }

    @Test
    void rejectsNonPositiveQuantity() {
        UUID productId = UUID.randomUUID();
        CreateOrderRequest request = requestWithItems(List.of(new OrderItemRequest(productId, 0)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("quantity must be greater than 0");
    }

    @Test
    void rejectsItemForMissingProduct() {
        UUID productId = UUID.randomUUID();
        given(productRepository.findByIdAndActiveTrue(productId)).willReturn(java.util.Optional.empty());
        CreateOrderRequest request = requestWithItems(List.of(new OrderItemRequest(productId, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    @Test
    void rejectsItemWhenStockIsInsufficient() {
        Product product = new Product("Ositos", "ositos", new BigDecimal("2.50"), 2, category);
        given(productRepository.findByIdAndActiveTrue(any())).willReturn(java.util.Optional.of(product));
        CreateOrderRequest request = requestWithItems(List.of(new OrderItemRequest(UUID.randomUUID(), 3)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Insufficient stock for product: Ositos");
    }

    @Test
    void createsOrderAndReservesStock() {
        Product product = new Product("Ositos", "ositos", new BigDecimal("2.50"), 10, category);
        given(productRepository.findByIdAndActiveTrue(any())).willReturn(java.util.Optional.of(product));
        given(orderRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        CreateOrderRequest request = requestWithItems(List.of(new OrderItemRequest(UUID.randomUUID(), 3)));

        OrderResponse response = orderService.createOrder(request);

        assertThat(product.getStock()).isEqualTo(7);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("Ositos");
        assertThat(response.items().get(0).quantity()).isEqualTo(3);
        assertThat(response.totalAmount()).isEqualByComparingTo("7.50");
        assertThat(response.status()).isEqualTo("PENDING");
    }

    @Test
    void findsOrderByIdForOwningCustomer() {
        UUID orderId = UUID.randomUUID();
        given(orderRepository.findByIdAndCustomerEmail(orderId, "someone-else@example.com"))
                .willReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> orderService.findByIdForCustomer(orderId, "someone-else@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found: " + orderId);
    }
}
