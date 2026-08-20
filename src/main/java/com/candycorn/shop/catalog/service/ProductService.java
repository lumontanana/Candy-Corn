package com.candycorn.shop.catalog.service;

import com.candycorn.shop.catalog.dto.ProductResponse;
import com.candycorn.shop.catalog.dto.PageResponse;
import com.candycorn.shop.catalog.repository.ProductRepository;
import com.candycorn.shop.catalog.repository.ProductSpecifications;
import com.candycorn.shop.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public PageResponse<ProductResponse> findAllActive(
            String search,
            String categorySlug,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        var specification = ProductSpecifications.isActive();
        if (search != null && !search.isBlank()) {
            specification = specification.and(ProductSpecifications.nameContains(search.trim()));
        }
        if (categorySlug != null && !categorySlug.isBlank()) {
            specification = specification.and(ProductSpecifications.belongsToCategory(categorySlug.trim()));
        }
        if (minPrice != null) {
            specification = specification.and(ProductSpecifications.priceGreaterThanOrEqualTo(minPrice));
        }
        if (maxPrice != null) {
            specification = specification.and(ProductSpecifications.priceLessThanOrEqualTo(maxPrice));
        }
        return PageResponse.from(productRepository.findAll(specification, pageable).map(ProductResponse::from));
    }

    public ProductResponse findActiveById(UUID id) {
        return productRepository.findByIdAndActiveTrue(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public ProductResponse findActiveBySlug(String slug) {
        return productRepository.findBySlugAndActiveTrue(slug)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
    }

    public PageResponse<ProductResponse> findAllActiveByCategorySlug(String categorySlug, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return PageResponse.from(
                productRepository.findAllByCategorySlugAndActiveTrue(categorySlug, pageable)
                        .map(ProductResponse::from));
    }
}
