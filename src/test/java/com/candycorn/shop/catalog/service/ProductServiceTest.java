package com.candycorn.shop.catalog.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.candycorn.shop.common.exception.InvalidRequestException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProductServiceTest {

    private final ProductService productService = new ProductService(Mockito.mock(
            com.candycorn.shop.catalog.repository.ProductRepository.class));

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> productService.findAllActive(null, null, null, null, -1, 20))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("page must be greater than or equal to 0");
    }

    @Test
    void rejectsPageSizeAboveMaximum() {
        assertThatThrownBy(() -> productService.findAllActive(null, null, null, null, 0, 101))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("size must be between 1 and 100");
    }

    @Test
    void rejectsInvertedPriceRange() {
        assertThatThrownBy(() -> productService.findAllActive(
                null,
                null,
                new BigDecimal("5.00"),
                new BigDecimal("1.00"),
                0,
                20))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("minPrice must be less than or equal to maxPrice");
    }
}
