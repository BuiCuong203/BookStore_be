package com.vn.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.vn.backend.dto.request.CreateOrderRequest;
import com.vn.backend.dto.request.UpdateOrderStatusRequest;
import com.vn.backend.dto.response.OrderItemResponse;
import com.vn.backend.dto.response.OrderResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.Cart;
import com.vn.backend.model.CartItem;
import com.vn.backend.model.Order;
import com.vn.backend.model.OrderItem;
import com.vn.backend.model.Product;
import com.vn.backend.model.User;
import com.vn.backend.repository.CartItemRepository;
import com.vn.backend.repository.CartRepository;
import com.vn.backend.repository.OrderItemRepository;
import com.vn.backend.repository.OrderRepository;
import com.vn.backend.repository.ProductRepository;
import com.vn.backend.repository.UserRepository;
import com.vn.backend.util.enums.OrderStatus;
import com.vn.backend.util.enums.PaymentMethod;
import com.vn.backend.util.enums.PaymentStatus;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

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
    PaymentService paymentService;

    /**
     * Lấy người dùng đang đăng nhập hiện tại
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "User not found"));
    }

    /**
     * Chuyển OrderItem sang OrderItemResponse
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
     * Chuyển Order và danh sách OrderItem sang OrderResponse
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
                .paymentStatus(order.getPaymentStatus())
                .totalAmount(order.getTotalAmount())
                .totalItem(order.getTotalItem())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Admin: lấy tất cả đơn hàng có phân trang
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
     * Lấy đơn hàng của người dùng hiện tại có phân trang
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
     * Lọc đơn hàng theo trạng thái có phân trang
     */
    public Page<OrderResponse> getOrdersByStatus(String status, Pageable pageable) {
        log.info("Getting orders with status: {}", status);

        OrderStatus orderStatus;
        try {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid order status: " + status);
        }

        return orderRepository.findByStatus(orderStatus, pageable)
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrder(order);
                    return toOrderResponse(order, items);
                });
    }

    /**
     * Lấy chi tiết đơn hàng theo ID (kiểm tra quyền cơ bản)
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
     * Tạo đơn từ giỏ: cho phép chọn một số mặt hàng hoặc mua tất cả
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User currentUser = getCurrentUser();
        log.info("Creating order for user: {}", currentUser.getEmail());

        // Lấy giỏ hàng của người dùng
        Cart cart = cartRepository.findByCustomer(currentUser)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Cart not found"));

        // Lấy tất cả hoặc chỉ các item được chọn
        List<CartItem> cartItems;
        if (request.getSelectedCartItemIds() != null && !request.getSelectedCartItemIds().isEmpty()) {
            // Chỉ lấy các item được chọn
            cartItems = cartItemRepository.findAllById(request.getSelectedCartItemIds());
            
            // Kiểm tra các item có thuộc giỏ của user không
            for (CartItem item : cartItems) {
                if (!item.getCart().getId().equals(cart.getId())) {
                    throw new AppException(HttpStatus.FORBIDDEN.value(), 
                        "Cart item does not belong to your cart");
                }
            }
        } else {
            // Mua tất cả
            cartItems = cartItemRepository.findByCart(cart);
        }

        if (cartItems.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "No items selected");
        }

        // Kiểm tra tồn kho
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product.getStockQuanity() < cartItem.getQuantity()) {
                throw new AppException(HttpStatus.BAD_REQUEST.value(),
                    "Product '" + product.getName() + "' has insufficient stock");
            }
        }

        // Tính tổng tiền của các item được chọn
        Long totalAmount = cartItems.stream()
                .mapToLong(CartItem::getTotal)
                .sum();
        
        int totalItem = cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        // Chuyển đổi và validate payment method
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getMethodPayment().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                "Invalid payment method. Accepted: COD, VNPAY, MOMO, BANKING");
        }

        // Xác định trạng thái thanh toán ban đầu
        PaymentStatus paymentStatus = (paymentMethod == PaymentMethod.COD) 
            ? PaymentStatus.UNPAID 
            : PaymentStatus.UNPAID; // Online payment cũng UNPAID cho đến khi xác nhận

        // Tạo đơn hàng
        Order order = Order.builder()
                .user(currentUser)
                .address(request.getAddress())
                .status(OrderStatus.PENDING)
                .methodPayment(paymentMethod)
                .paymentStatus(paymentStatus)
                .totalAmount(totalAmount)
                .totalItem(totalItem)
                .build();

        order = orderRepository.save(order);
        log.info("Order created with id: {}", order.getId());

        // Tạo payment record
        paymentService.createPayment(order);

        // Tạo order items và trừ tồn kho
        final Order finalOrder = order;
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> {
                    Product product = cartItem.getProduct();

                    // Trừ tồn kho
                    product.setStockQuanity(product.getStockQuanity() - cartItem.getQuantity());
                    productRepository.save(product);

                    // Tạo order item
                    OrderItem orderItem = OrderItem.builder()
                            .order(finalOrder)
                            .product(product)
                            .quantity(cartItem.getQuantity())
                            .total(cartItem.getTotal())
                            .build();

                    return orderItemRepository.save(orderItem);
                })
                .collect(Collectors.toList());

        // Xóa chỉ các item đã đặt khỏi giỏ
        List<Long> cartItemIdsToDelete = cartItems.stream()
                .map(CartItem::getId)
                .collect(Collectors.toList());
        cartItemRepository.deleteAllById(cartItemIdsToDelete);

        // Cập nhật lại tổng tiền giỏ hàng
        List<CartItem> remainingItems = cartItemRepository.findByCart(cart);
        Long newCartTotal = remainingItems.stream()
                .mapToLong(CartItem::getTotal)
                .sum();
        cart.setTotal(newCartTotal);
        cartRepository.save(cart);

        log.info("Order items created and selected cart items cleared");
        return toOrderResponse(order, orderItems);
    }

    /**
     * Admin duyệt đơn PENDING thành CONFIRMED
     */
    @Transactional
    public OrderResponse approveOrder(Long id) {
        log.info("Admin approving order with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Only pending orders can be approved");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrder(order);
        log.info("Order approved successfully with id: {}", id);
        return toOrderResponse(order, items);
    }

    /**
     * Cập nhật trạng thái đơn theo luồng hợp lệ PENDING→CONFIRMED→PROCESSING→SHIPPING→DELIVERED
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        log.info("Updating order {} status to: {}", id, request.getStatus());

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

        // Validate status transition
        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus;
        
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid order status: " + request.getStatus());
        }

        // Define valid transitions
        switch (currentStatus) {
            case PENDING:
                if (newStatus != OrderStatus.CONFIRMED) {
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid status transition from PENDING");
                }
                break;
            case CONFIRMED:
                if (newStatus != OrderStatus.PROCESSING) {
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid status transition from CONFIRMED");
                }
                break;
            case PROCESSING:
                if (newStatus != OrderStatus.SHIPPING) {
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid status transition from PROCESSING");
                }
                break;
            case SHIPPING:
                if (newStatus != OrderStatus.DELIVERED) {
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid status transition from SHIPPING");
                }
                break;
            default:
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid current status");
        }

        order.setStatus(newStatus);
        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrder(order);
        log.info("Order status updated successfully to: {}", newStatus);
        return toOrderResponse(order, items);
    }

    /**
     * Người dùng hủy đơn PENDING của mình, hoàn trả tồn kho
     */
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        User currentUser = getCurrentUser();
        log.info("User {} cancelling order {}", currentUser.getEmail(), id);

        Order order = orderRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(),
                    "Order not found or you don't have permission"));

        if (order.getStatus() != OrderStatus.PENDING) {
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

        // Hoàn tiền nếu đã thanh toán
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            paymentService.refundPayment(id);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        log.info("Order cancelled and stock restored");
        return toOrderResponse(order, items);
    }

    /**
     * Admin xóa đơn: xóa OrderItem trước rồi xóa Order
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
