package com.candycorn.shop.catalog.controller;

import com.candycorn.shop.catalog.dto.CategoryResponse;
import com.candycorn.shop.catalog.dto.ProductResponse;
import com.candycorn.shop.catalog.service.CategoryService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> findAll() {
        return categoryService.findAllActive();
    }

    @GetMapping("/{id}/products")
    public List<ProductResponse> findProducts(@PathVariable UUID id) {
        return categoryService.findProductsByCategory(id);
    }
}
