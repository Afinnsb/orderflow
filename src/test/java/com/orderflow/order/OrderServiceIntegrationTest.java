package com.orderflow.order;

import com.orderflow.exception.InsufficientStockException;
import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.product.Product;
import com.orderflow.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void shouldCreateOrderAndPersistItToDatabase() {

        Product keyboard = new Product();
        keyboard.setName("Integration Keyboard");
        keyboard.setDescription("Integration test keyboard");
        keyboard.setPrice(new BigDecimal("2500.00"));
        keyboard.setStock(10);

        Product savedProduct = productRepository.save(keyboard);

        CreateOrderRequest request = new CreateOrderRequest(
                1001L,
                List.of(
                        new CreateOrderItemRequest(
                                savedProduct.getId(),
                                2
                        )
                )
        );

        var response = orderService.createOrder(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.status())
                .isEqualTo(OrderStatus.CREATED);

        assertThat(response.totalPrice())
                .isEqualByComparingTo("5000.00");

        Product updatedProduct = productRepository
                .findById(savedProduct.getId())
                .orElseThrow();

        assertThat(updatedProduct.getStock())
                .isEqualTo(8);

        assertThat(orderRepository.count())
                .isEqualTo(1);
    }

    @Test
    void shouldRollbackEverythingWhenStockIsInsufficient() {

        Product keyboard = new Product();
        keyboard.setName("Rollback Keyboard");
        keyboard.setPrice(new BigDecimal("2500.00"));
        keyboard.setStock(10);

        Product mouse = new Product();
        mouse.setName("Rollback Mouse");
        mouse.setPrice(new BigDecimal("1500.00"));
        mouse.setStock(5);

        Product savedKeyboard = productRepository.save(keyboard);
        Product savedMouse = productRepository.save(mouse);

        CreateOrderRequest request = new CreateOrderRequest(
                1002L,
                List.of(
                        new CreateOrderItemRequest(
                                savedKeyboard.getId(),
                                1
                        ),
                        new CreateOrderItemRequest(
                                savedMouse.getId(),
                                100
                        )
                )
        );

        assertThatThrownBy(() ->
                orderService.createOrder(request)
        )
                .isInstanceOf(InsufficientStockException.class);

        Product keyboardAfterRollback = productRepository
                .findById(savedKeyboard.getId())
                .orElseThrow();

        Product mouseAfterRollback = productRepository
                .findById(savedMouse.getId())
                .orElseThrow();

        assertThat(keyboardAfterRollback.getStock())
                .isEqualTo(10);

        assertThat(mouseAfterRollback.getStock())
                .isEqualTo(5);

        assertThat(orderRepository.count())
                .isZero();
    }
}