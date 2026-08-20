package com.candycorn.shop.catalog.repository;

import com.candycorn.shop.catalog.entity.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByActiveTrueOrderByNameAsc();

    Optional<Category> findByIdAndActiveTrue(UUID id);

    Optional<Category> findBySlugAndActiveTrue(String slug);
}
