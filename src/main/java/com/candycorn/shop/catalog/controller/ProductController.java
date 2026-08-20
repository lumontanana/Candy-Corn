package com.candycorn.shop.catalog.controller;

import com.candycorn.shop.catalog.dto.ProductResponse;
import com.candycorn.shop.catalog.dto.PageResponse;
import com.candycorn.shop.catalog.service.ProductService;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PageResponse<ProductResponse> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.findAllActive(search, category, minPrice, maxPrice, page, size);
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable UUID id) {
        return productService.findActiveById(id);
    }

    @GetMapping("/slug/{slug}")
    public ProductResponse findBySlug(@PathVariable String slug) {
        return productService.findActiveBySlug(slug);
    }
}
