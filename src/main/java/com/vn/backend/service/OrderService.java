package com.vn.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.vn.backend.dto.request.CreateOrderRequest;
import com.vn.backend.dto.request.UpdateOrderStatusRequest;
import com.vn.backend.dto.response.OrderDashboardResponse;
import com.vn.backend.dto.response.OrderItemResponse;
import com.vn.backend.dto.response.OrderResponse;
import com.vn.backend.dto.response.PagedResponse;
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
    public PagedResponse<OrderResponse> getAllOrders(String keyword, Pageable pageable) {
        Pageable pageableWithDefaultSort = pageable;
        if (pageable.getSort().isUnsorted()) {
            Sort defaultSort = Sort.by(Sort.Direction.DESC, "id");
            pageableWithDefaultSort = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    defaultSort
            );
        }

        Page<Order> orders;

        if (keyword != null && !keyword.trim().isEmpty()) {
            orders = orderRepository.findByKeyword(keyword.trim(), pageableWithDefaultSort);
        } else {
            orders = orderRepository.findAll(pageableWithDefaultSort);
        }

        List<OrderResponse> orderResponses = orders.getContent().stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrder(order);
                    return toOrderResponse(order, items);
                })
                .collect(Collectors.toList());

        PagedResponse<OrderResponse> response = PagedResponse.<OrderResponse>builder()
                .data(orderResponses)
                .totalElements(orders.getTotalElements())
                .totalPages(orders.getTotalPages())
                .currentPage(orders.getNumber())
                .pageSize(orders.getSize())
                .hasNext(orders.hasNext())
                .hasPrevious(orders.hasPrevious())
                .build();

        return response;
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
        // COD: UNPAID (chờ giao hàng rồi thu tiền)
        // Online payment (VNPAY, MOMO, BANKING): PENDING (chờ xác nhận từ gateway)
        PaymentStatus paymentStatus = (paymentMethod == PaymentMethod.COD)
                ? PaymentStatus.UNPAID
                : PaymentStatus.PENDING;

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

        // Tạo payment record chỉ cho COD (online payment sẽ tạo khi gọi gateway)
        if (paymentMethod == PaymentMethod.COD) {
            paymentService.createPayment(order);
        }

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
                if (newStatus != OrderStatus.CONFIRMED && newStatus != OrderStatus.CANCELLED) {
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                        "From PENDING, can only move to CONFIRMED or CANCELLED");
                }
                break;
            case CONFIRMED:
                if (newStatus != OrderStatus.PROCESSING && newStatus != OrderStatus.CANCELLED) {
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                        "From CONFIRMED, can only move to PROCESSING or CANCELLED");
                }
                break;
            case PROCESSING:
                if (newStatus != OrderStatus.SHIPPING) {
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                        "From PROCESSING, can only move to SHIPPING");
                }
                break;
            case SHIPPING:
                if (newStatus != OrderStatus.DELIVERED) {
                    throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                        "From SHIPPING, can only move to DELIVERED");
                }
                break;
            case DELIVERED:
                throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                    "Order already delivered, no further status changes allowed");
            case CANCELLED:
                throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                    "Cancelled orders cannot be updated");
            default:
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid current status");
        }

        order.setStatus(newStatus);
        
        // Tự động xác nhận thanh toán COD khi giao hàng thành công
        if (newStatus == OrderStatus.DELIVERED && order.getMethodPayment() == PaymentMethod.COD
                && order.getPaymentStatus() == PaymentStatus.UNPAID) {
            log.info("Auto-confirming COD payment for delivered order {}", id);
            try {
                paymentService.confirmCODPayment(id);
                order.setPaymentStatus(PaymentStatus.PAID);
            } catch (Exception e) {
                log.error("Failed to auto-confirm COD payment for order {}", id, e);
            }
        }
        
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

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                    "Only pending or confirmed orders can be cancelled by customer");
        }

        // Restore product stock
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem item : items) {
            Product product = item.getProduct();
            product.setStockQuanity(product.getStockQuanity() + item.getQuantity());
            productRepository.save(product);
            log.debug("Restored {} units of product {} (ID: {})", 
                item.getQuantity(), product.getName(), product.getId());
        }

        // Hoàn tiền nếu đã thanh toán
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("Refunding payment for cancelled order {}", id);
            try {
                paymentService.refundPayment(id);
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            } catch (Exception e) {
                log.error("Failed to refund payment for order {}", id, e);
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Order cancelled but refund failed. Please contact support.");
            }
        } else if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            // Nếu payment đang pending (online payment chưa hoàn tất), đánh dấu failed
            order.setPaymentStatus(PaymentStatus.FAILED);
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

    public OrderDashboardResponse getDashboardStats() {
        // 1. Lấy tổng doanh thu
        Double totalRevenue = orderRepository.sumTotalAmount();
        if (totalRevenue == null) totalRevenue = 0.0;

        // 2. Lấy thống kê theo trạng thái
        List<Object[]> statusCounts = orderRepository.countOrdersByStatus();

        long totalOrders = 0;
        long deliveringOrders = 0;
        List<OrderDashboardResponse.OrderStatusStat> stats = new ArrayList<>();

        for (Object[] row : statusCounts) {
            OrderStatus status = (OrderStatus) row[0]; // Giả sử status là Enum
            Long count = (Long) row[1];

            // Cộng dồn tổng số đơn
            totalOrders += count;

            // Check đơn đang giao
            if (status == OrderStatus.DELIVERED) {
                deliveringOrders = count;
            }

            // Add vào list cho biểu đồ
            stats.add(OrderDashboardResponse.OrderStatusStat.builder()
                    .status(status.name())
                    .count(count)
                    .build());
        }

        return OrderDashboardResponse.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .deliveringOrders(deliveringOrders)
                .statusBreakdown(stats)
                .build();
    }
}
