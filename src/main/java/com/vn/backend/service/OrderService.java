package com.vn.backend.service;

import com.vn.backend.dto.request.CreateOrderRequest;
import com.vn.backend.dto.request.UpdateOrderStatusRequest;
import com.vn.backend.dto.response.OrderItemResponse;
import com.vn.backend.dto.response.OrderResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.*;
import com.vn.backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderService {

    OrderRepository orderRepository;
    OrderItemRepository orderItemRepository;
    CartRepository cartRepository;
    CartItemRepository cartItemRepository;
    ProductRepository productRepository;
    UserRepository userRepository;

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "User not found"));
    }

    /**
     * Convert OrderItem to OrderItemResponse
     */
    private OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        Product product = orderItem.getProduct();
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productPrice(product.getPrice())
                .productDiscount(product.getDiscount())
                .quantity(orderItem.getQuantity())
                .total(orderItem.getTotal())
                .build();
    }

    /**
     * Convert Order to OrderResponse
     */
    private OrderResponse toOrderResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getUser().getId())
                .customerName(order.getUser().getFullName())
                .customerEmail(order.getUser().getEmail())
                .address(order.getAddress())
                .status(order.getStatus())
                .methodPayment(order.getMethodPayment())
                .totalAmount(order.getTotalAmount())
                .totalItem(order.getTotalItem())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Get all orders with pagination (Admin)
     */
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        log.info("Getting all orders with pagination");
        return orderRepository.findAll(pageable)
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrder(order);
                    return toOrderResponse(order, items);
                });
    }

    /**
     * Get user's orders with pagination
     */
    public Page<OrderResponse> getUserOrders(Pageable pageable) {
        User currentUser = getCurrentUser();
        log.info("Getting orders for user: {}", currentUser.getEmail());

        return orderRepository.findByUser(currentUser, pageable)
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrder(order);
                    return toOrderResponse(order, items);
                });
    }

    /**
     * Get orders by status
     */
    public Page<OrderResponse> getOrdersByStatus(String status, Pageable pageable) {
        log.info("Getting orders with status: {}", status);

        return orderRepository.findByStatus(status, pageable)
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrder(order);
                    return toOrderResponse(order, items);
                });
    }

    /**
     * Get order by ID
     */
    public OrderResponse getOrderById(Long id) {
        User currentUser = getCurrentUser();
        log.info("Getting order with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

        // Check if user owns this order (unless admin)
        if (!order.getUser().getId().equals(currentUser.getId())) {
            // TODO: Check if user is admin, if not throw forbidden exception
            // For now, we'll allow it
        }

        List<OrderItem> items = orderItemRepository.findByOrder(order);
        return toOrderResponse(order, items);
    }

    /**
     * Create order from cart
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User currentUser = getCurrentUser();
        log.info("Creating order for user: {}", currentUser.getEmail());

        // Get user's cart
        Cart cart = cartRepository.findByCustomer(currentUser)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Cart not found"));

        // Get cart items
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        if (cartItems.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Cart is empty");
        }

        // Validate stock for all products
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product.getStockQuanity() < cartItem.getQuantity()) {
                throw new AppException(HttpStatus.BAD_REQUEST.value(),
                    "Product '" + product.getName() + "' has insufficient stock");
            }
        }

        // Calculate total
        Long totalAmount = cart.getTotal();
        int totalItem = cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        // Create order
        Order order = Order.builder()
                .user(currentUser)
                .address(request.getAddress())
                .status("PENDING")
                .methodPayment(request.getMethodPayment())
                .totalAmount(totalAmount)
                .totalItem(totalItem)
                .build();

        order = orderRepository.save(order);
        log.info("Order created with id: {}", order.getId());

        // Create order items and update product stock
        final Order finalOrder = order;
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> {
                    Product product = cartItem.getProduct();

                    // Update product stock
                    product.setStockQuanity(product.getStockQuanity() - cartItem.getQuantity());
                    productRepository.save(product);

                    // Create order item
                    OrderItem orderItem = OrderItem.builder()
                            .order(finalOrder)
                            .product(product)
                            .quantity(cartItem.getQuantity())
                            .total(cartItem.getTotal())
                            .build();

                    return orderItemRepository.save(orderItem);
                })
                .collect(Collectors.toList());

        // Clear cart after creating order
        cartItemRepository.deleteByCartId(cart.getId());
        cart.setTotal(0L);
        cartRepository.save(cart);

        log.info("Order items created and cart cleared");
        return toOrderResponse(order, orderItems);
    }

    /**
     * Update order status
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        log.info("Updating order {} status to: {}", id, request.getStatus());

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

        // Validate status transition
        String currentStatus = order.getStatus();
        String newStatus = request.getStatus().toUpperCase();

        // Business logic for status transitions
        if ("CANCELLED".equals(currentStatus)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                "Cannot update status of cancelled order");
        }

        if ("DELIVERED".equals(currentStatus)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                "Cannot update status of delivered order");
        }

        // If cancelling, restore product stock
        if ("CANCELLED".equals(newStatus) && !"CANCELLED".equals(currentStatus)) {
            List<OrderItem> items = orderItemRepository.findByOrder(order);
            for (OrderItem item : items) {
                Product product = item.getProduct();
                product.setStockQuanity(product.getStockQuanity() + item.getQuantity());
                productRepository.save(product);
            }
            log.info("Product stock restored for cancelled order");
        }

        order.setStatus(newStatus);
        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrder(order);
        return toOrderResponse(order, items);
    }

    /**
     * Cancel order (User)
     */
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        User currentUser = getCurrentUser();
        log.info("User {} cancelling order {}", currentUser.getEmail(), id);

        Order order = orderRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(),
                    "Order not found or you don't have permission"));

        if (!"PENDING".equals(order.getStatus())) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                "Only pending orders can be cancelled");
        }

        // Restore product stock
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem item : items) {
            Product product = item.getProduct();
            product.setStockQuanity(product.getStockQuanity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus("CANCELLED");
        order = orderRepository.save(order);

        log.info("Order cancelled and stock restored");
        return toOrderResponse(order, items);
    }

    /**
     * Delete order (Admin only)
     */
    @Transactional
    public void deleteOrder(Long id) {
        log.info("Deleting order with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

        // Delete order items first
        orderItemRepository.deleteByOrderId(id);

        // Delete order
        orderRepository.delete(order);

        log.info("Order deleted successfully with id: {}", id);
    }
}
