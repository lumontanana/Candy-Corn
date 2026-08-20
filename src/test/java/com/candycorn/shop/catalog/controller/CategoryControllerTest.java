package com.candycorn.shop.catalog.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.candycorn.shop.catalog.dto.CategoryResponse;
import com.candycorn.shop.catalog.dto.PageResponse;
import com.candycorn.shop.catalog.service.CategoryService;
import com.candycorn.shop.common.exception.GlobalExceptionHandler;
import com.candycorn.shop.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CategoryControllerTest {

    private final CategoryService categoryService = org.mockito.Mockito.mock(CategoryService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CategoryController controller = new CategoryController(categoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsActiveCategories() throws Exception {
        given(categoryService.findAllActive())
                .willReturn(List.of(new CategoryResponse(UUID.randomUUID(), "Gominolas", "gominolas")));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Gominolas"))
                .andExpect(jsonPath("$[0].slug").value("gominolas"));
    }

    @Test
    void returnsNotFoundWhenCategoryDoesNotExist() throws Exception {
        UUID categoryId = UUID.randomUUID();
        given(categoryService.findProductsByCategory(categoryId, 0, 20))
                .willThrow(new ResourceNotFoundException("Category not found: " + categoryId));

        mockMvc.perform(get("/api/v1/categories/{id}/products", categoryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }
}
