package com.candycorn.shop.catalog.dto;

import com.candycorn.shop.catalog.entity.Product;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        String imageUrl,
        int stock,
        CategoryResponse category) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getStock(),
                CategoryResponse.from(product.getCategory()));
    }
}
