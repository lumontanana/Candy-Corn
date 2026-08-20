package com.candycorn.shop.catalog.repository;

import com.candycorn.shop.catalog.entity.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findAllByActiveTrue(Pageable pageable);

    Optional<Product> findByIdAndActiveTrue(UUID id);

    Optional<Product> findBySlugAndActiveTrue(String slug);

    Page<Product> findAllByCategorySlugAndActiveTrue(String categorySlug, Pageable pageable);
}
