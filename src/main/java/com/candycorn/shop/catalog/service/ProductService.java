package com.candycorn.shop.catalog.service;

import com.candycorn.shop.catalog.dto.ProductResponse;
import com.candycorn.shop.catalog.repository.ProductRepository;
import com.candycorn.shop.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> findAllActive() {
        return productRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(ProductResponse::from)
                .toList();
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

    public List<ProductResponse> findAllActiveByCategorySlug(String categorySlug) {
        return productRepository.findAllByCategorySlugAndActiveTrueOrderByNameAsc(categorySlug).stream()
                .map(ProductResponse::from)
                .toList();
    }
}
