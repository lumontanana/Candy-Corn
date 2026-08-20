package com.candycorn.shop.catalog.service;

import com.candycorn.shop.catalog.dto.CategoryResponse;
import com.candycorn.shop.catalog.dto.ProductResponse;
import com.candycorn.shop.catalog.dto.PageResponse;
import com.candycorn.shop.catalog.repository.CategoryRepository;
import com.candycorn.shop.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductService productService;

    public CategoryService(CategoryRepository categoryRepository, ProductService productService) {
        this.categoryRepository = categoryRepository;
        this.productService = productService;
    }

    public List<CategoryResponse> findAllActive() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public PageResponse<ProductResponse> findProductsByCategory(UUID categoryId, int page, int size) {
        String slug = categoryRepository.findByIdAndActiveTrue(categoryId)
                .map(category -> category.getSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        return productService.findAllActiveByCategorySlug(slug, page, size);
    }
}
