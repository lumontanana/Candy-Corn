package com.candycorn.shop.catalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.candycorn.shop.catalog.entity.Category;
import com.candycorn.shop.catalog.entity.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CatalogRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void persistsProductAndFindsItByActiveSlug() {
        Category category = categoryRepository.saveAndFlush(new Category("Gominolas", "gominolas"));
        Product product = new Product("Ositos", "ositos", new BigDecimal("2.50"), 20, category);
        productRepository.saveAndFlush(product);

        assertThat(product.getId()).isNotNull();
        assertThat(productRepository.findBySlugAndActiveTrue("ositos"))
                .isPresent()
                .get()
                .extracting(Product::getName)
                .isEqualTo("Ositos");
    }
}
