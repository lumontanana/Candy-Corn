package com.candycorn.shop.order.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.candycorn.shop.common.exception.GlobalExceptionHandler;
import com.candycorn.shop.common.exception.InvalidRequestException;
import com.candycorn.shop.common.exception.ResourceNotFoundException;
import com.candycorn.shop.order.dto.AddressRequest;
import com.candycorn.shop.order.dto.AddressResponse;
import com.candycorn.shop.order.dto.CreateOrderRequest;
import com.candycorn.shop.order.dto.OrderItemRequest;
import com.candycorn.shop.order.dto.OrderItemResponse;
import com.candycorn.shop.order.dto.OrderResponse;
import com.candycorn.shop.order.service.OrderService;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OrderControllerTest {

    private final OrderService orderService = mock(OrderService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController(orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private CreateOrderRequest sampleRequest() {
        AddressRequest address = new AddressRequest("Ana", "Calle Falsa 123", "Madrid", "28080", "ES", null);
        return new CreateOrderRequest("Ana", "ana@example.com", address,
                List.of(new OrderItemRequest(UUID.randomUUID(), 2)));
    }

    private OrderResponse sampleResponse(UUID orderId) {
        AddressResponse address = new AddressResponse("Ana", "Calle Falsa 123", "Madrid", "28080", "ES", null);
        OrderItemResponse item = new OrderItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Ositos", new BigDecimal("2.50"), 2, new BigDecimal("5.00"));
        return new OrderResponse(orderId, "Ana", "ana@example.com", "PENDING", address, List.of(item),
                new BigDecimal("5.00"), Instant.now());
    }

    @Test
    void createsOrderAndReturnsCreated() throws Exception {
        CreateOrderRequest request = sampleRequest();
        UUID orderId = UUID.randomUUID();
        given(orderService.createOrder(request)).willReturn(sampleResponse(orderId));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].productName").value("Ositos"));
    }

    @Test
    void returnsBadRequestWhenOrderIsInvalid() throws Exception {
        CreateOrderRequest request = sampleRequest();
        given(orderService.createOrder(request))
                .willThrow(new InvalidRequestException("An order must contain at least one item"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void returnsOrderForOwningCustomer() throws Exception {
        UUID orderId = UUID.randomUUID();
        given(orderService.findByIdForCustomer(orderId, "ana@example.com")).willReturn(sampleResponse(orderId));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId).param("email", "ana@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerEmail").value("ana@example.com"));
    }

    @Test
    void returnsNotFoundWhenOrderDoesNotBelongToCustomer() throws Exception {
        UUID orderId = UUID.randomUUID();
        given(orderService.findByIdForCustomer(orderId, "someone-else@example.com"))
                .willThrow(new ResourceNotFoundException("Order not found: " + orderId));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId).param("email", "someone-else@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }
}
