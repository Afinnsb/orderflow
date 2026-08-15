package com.orderflow.order;

import com.orderflow.exception.InsufficientStockException;
import com.orderflow.exception.InvalidOrderStatusTransitionException;
import com.orderflow.exception.ResourceNotFoundException;
import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.product.Product;
import com.orderflow.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private Product keyboard;
    private Product mouse;

    @BeforeEach
    void setUp() {

        keyboard = new Product();
        keyboard.setName("Mechanical Keyboard");
        keyboard.setDescription("RGB mechanical keyboard");
        keyboard.setPrice(new BigDecimal("2499.90"));
        keyboard.setStock(25);

        mouse = new Product();
        mouse.setName("Gaming Mouse");
        mouse.setDescription("Wireless gaming mouse");
        mouse.setPrice(new BigDecimal("1499.90"));
        mouse.setStock(10);
    }

    @Test
    void shouldCreateOrderAndDecreaseStock() {

        CreateOrderRequest request = new CreateOrderRequest(
                1001L,
                List.of(
                        new CreateOrderItemRequest(1L, 2),
                        new CreateOrderItemRequest(2L, 1)
                )
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(keyboard));

        when(productRepository.findById(2L))
                .thenReturn(Optional.of(mouse));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    return order;
                });

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.customerId())
                .isEqualTo(1001L);

        assertThat(response.status())
                .isEqualTo(OrderStatus.CREATED);

        assertThat(response.totalPrice())
                .isEqualByComparingTo("6499.70");

        assertThat(response.items())
                .hasSize(2);

        assertThat(keyboard.getStock())
                .isEqualTo(23);

        assertThat(mouse.getStock())
                .isEqualTo(9);

        verify(productRepository).findById(1L);
        verify(productRepository).findById(2L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldThrowExceptionWhenProductDoesNotExist() {

        CreateOrderRequest request = new CreateOrderRequest(
                1001L,
                List.of(
                        new CreateOrderItemRequest(999L, 1)
                )
        );

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.createOrder(request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found: 999");

        verify(productRepository).findById(999L);

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldThrowExceptionWhenStockIsInsufficient() {

        CreateOrderRequest request = new CreateOrderRequest(
                1001L,
                List.of(
                        new CreateOrderItemRequest(1L, 100)
                )
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(keyboard));

        assertThatThrownBy(() ->
                orderService.createOrder(request)
        )
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage(
                        "Insufficient stock for product: Mechanical Keyboard"
                );

        assertThat(keyboard.getStock())
                .isEqualTo(25);

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldUpdateOrderStatus() {

        Order order = new Order();
        order.setCustomerId(1001L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(new BigDecimal("2499.90"));

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.CONFIRMED
                );

        assertThat(response.status())
                .isEqualTo(OrderStatus.CONFIRMED);

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);

        verify(orderRepository).findById(1L);
        verify(orderRepository).save(order);
    }

    @Test
    void shouldRejectInvalidOrderStatusTransition() {

        Order order = new Order();
        order.setCustomerId(1001L);
        order.setStatus(OrderStatus.DELIVERED);
        order.setTotalPrice(new BigDecimal("2499.90"));

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.CANCELLED
                )
        )
                .isInstanceOf(
                        InvalidOrderStatusTransitionException.class
                )
                .hasMessage(
                        "Invalid order status transition: DELIVERED -> CANCELLED"
                );

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldRejectStatusUpdateWhenOrderDoesNotExist() {

        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.updateOrderStatus(
                        999L,
                        OrderStatus.CONFIRMED
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found: 999");

        verify(orderRepository, never())
                .save(any(Order.class));
    }
}