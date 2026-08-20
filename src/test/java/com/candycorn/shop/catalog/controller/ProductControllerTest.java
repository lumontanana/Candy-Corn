package com.candycorn.shop.catalog.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.candycorn.shop.catalog.dto.CategoryResponse;
import com.candycorn.shop.catalog.dto.PageResponse;
import com.candycorn.shop.catalog.dto.ProductResponse;
import com.candycorn.shop.catalog.service.ProductService;
import com.candycorn.shop.common.exception.GlobalExceptionHandler;
import com.candycorn.shop.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProductControllerTest {

    private final ProductService productService = org.mockito.Mockito.mock(ProductService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(productService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsActiveProductsAsJson() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ProductResponse response = new ProductResponse(
                productId,
                "Ositos",
                "ositos",
                "Gominolas de fresa",
                new BigDecimal("2.50"),
                null,
                20,
                new CategoryResponse(categoryId, "Gominolas", "gominolas"));
        given(productService.findAllActive(0, 20))
                .willReturn(new PageResponse<>(List.of(response), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.content[0].name").value("Ositos"))
                .andExpect(jsonPath("$.content[0].category.slug").value("gominolas"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void returnsNotFoundErrorWhenProductDoesNotExist() throws Exception {
        UUID productId = UUID.randomUUID();
        given(productService.findActiveById(productId))
                .willThrow(new ResourceNotFoundException("Product not found: " + productId));

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }
}
