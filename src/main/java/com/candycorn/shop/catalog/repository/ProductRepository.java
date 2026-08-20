package com.candycorn.shop.catalog.repository;

import com.candycorn.shop.catalog.entity.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlugAndActiveTrue(String slug);
}
