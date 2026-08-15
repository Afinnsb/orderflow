package com.orderflow.order;

import com.orderflow.exception.InsufficientStockException;
import com.orderflow.exception.InvalidOrderStatusTransitionException;
import com.orderflow.exception.ResourceNotFoundException;
import com.orderflow.order.dto.CreateOrderItemRequest;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderItemResponse;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.product.Product;
import com.orderflow.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        Order order = new Order();
        order.setCustomerId(request.customerId());

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.items()) {

            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found: " + itemRequest.productId()
                            )
                    );

            try {
                product.decreaseStock(itemRequest.quantity());
            } catch (IllegalArgumentException exception) {
                throw new InsufficientStockException(
                    "Insufficient stock for product: " + product.getName()
                );
            }

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.quantity());
            orderItem.setUnitPrice(product.getPrice());

            order.addItem(orderItem);

            totalPrice = totalPrice.add(subtotal);
        }

        order.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(order);

        return toResponse(savedOrder);
    }

    private OrderResponse toResponse(Order order) {

        List<OrderItemResponse> items = new ArrayList<>();

        for (OrderItem item : order.getItems()) {

            BigDecimal subtotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            items.add(
                    new OrderItemResponse(
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            subtotal
                    )
            );
        }

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getCreatedAt(),
                items
        );
    }

    @Transactional(readOnly = true)
public List<OrderResponse> getAllOrders() {
    return orderRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
}

@Transactional(readOnly = true)
public OrderResponse getOrderById(Long id) {
    Order order = orderRepository.findById(id)
            .orElseThrow(() ->
                    new ResourceNotFoundException("Order not found: " + id)
            );

    return toResponse(order);
}

@Transactional
public OrderResponse updateOrderStatus(
        Long orderId,
        OrderStatus newStatus
) {

    Order order = orderRepository.findById(orderId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Order not found: " + orderId
                    )
            );

    OrderStatus currentStatus = order.getStatus();

    if (!isValidTransition(currentStatus, newStatus)) {
        throw new InvalidOrderStatusTransitionException(
                "Invalid order status transition: "
                        + currentStatus
                        + " -> "
                        + newStatus
        );
    }

    order.setStatus(newStatus);

    Order savedOrder = orderRepository.save(order);

    return toResponse(savedOrder);
}

private boolean isValidTransition(
        OrderStatus currentStatus,
        OrderStatus newStatus
) {

    return switch (currentStatus) {

        case CREATED ->
                newStatus == OrderStatus.CONFIRMED
                        || newStatus == OrderStatus.CANCELLED;

        case CONFIRMED ->
                newStatus == OrderStatus.SHIPPED
                        || newStatus == OrderStatus.CANCELLED;

        case SHIPPED ->
                newStatus == OrderStatus.DELIVERED;

        case DELIVERED, CANCELLED ->
                false;
    };
}
}