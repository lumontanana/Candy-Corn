package com.candycorn.shop.catalog.repository;

import com.candycorn.shop.catalog.entity.Product;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> isActive() {
        return (root, query, builder) -> builder.isTrue(root.get("active"));
    }

    public static Specification<Product> nameContains(String search) {
        return (root, query, builder) -> builder.like(
                builder.lower(root.get("name")),
                "%" + search.toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<Product> belongsToCategory(String categorySlug) {
        return (root, query, builder) -> builder.equal(
                root.get("category").get("slug"),
                categorySlug);
    }

    public static Specification<Product> priceGreaterThanOrEqualTo(BigDecimal minPrice) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqualTo(BigDecimal maxPrice) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}