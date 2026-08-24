package com.candycorn.shop.order.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.candycorn.shop.catalog.entity.Category;
import com.candycorn.shop.catalog.entity.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderTest {

    private final Category category = new Category("Gominolas", "gominolas");
    private final Product product = new Product("Ositos", "ositos", new BigDecimal("2.50"), 10, category);

    private Order newOrder() {
        Address address = new Address("Ana", "Calle Falsa 123", "Madrid", "28080", "ES", "600000000");
        return new Order("Ana", "ana@example.com", address);
    }

    @Test
    void addItemCopiesProductNameAndPriceAsSnapshot() {
        Order order = newOrder();
        order.addItem(product, 3);

        OrderItem item = order.getItems().get(0);
        assertThat(item.getProductName()).isEqualTo("Ositos");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("2.50");
        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    void addItemRejectsNonPositiveQuantity() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.addItem(product, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be greater than 0");
    }

    @Test
    void totalAmountSumsItemSubtotals() {
        Order order = newOrder();
        order.addItem(product, 2);
        order.addItem(product, 1);

        assertThat(order.getTotalAmount()).isEqualByComparingTo("7.50");
    }

    @Test
    void newOrderStartsPending() {
        assertThat(newOrder().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void allowsValidStatusTransition() {
        Order order = newOrder();
        order.changeStatus(OrderStatus.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void rejectsInvalidStatusTransition() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.changeStatus(OrderStatus.SHIPPED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot move order from PENDING to SHIPPED");
    }

    @Test
    void rejectsTransitionFromTerminalState() {
        Order order = newOrder();
        order.changeStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> order.changeStatus(OrderStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class);
    }
}
