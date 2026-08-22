package com.candycorn.shop.catalog.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {

    private final Category category = new Category("Gominolas", "gominolas");

    @Test
    void changeStockAcceptsZero() {
        Product product = new Product("Ositos", "ositos", new BigDecimal("2.50"), 10, category);
        product.changeStock(0);
        assertThat(product.getStock()).isZero();
    }

    @Test
    void changeStockRejectsNegativeValue() {
        Product product = new Product("Ositos", "ositos", new BigDecimal("2.50"), 10, category);
        assertThatThrownBy(() -> product.changeStock(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stock cannot be negative");
    }
}
