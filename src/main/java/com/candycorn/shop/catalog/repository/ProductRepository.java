package com.candycorn.shop.catalog.repository;

import com.candycorn.shop.catalog.entity.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByActiveTrueOrderByNameAsc();

    Optional<Product> findByIdAndActiveTrue(UUID id);

    Optional<Product> findBySlugAndActiveTrue(String slug);
}
