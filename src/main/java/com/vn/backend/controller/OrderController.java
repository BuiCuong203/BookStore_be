package com.vn.backend.controller;

import com.vn.backend.dto.request.CreateOrderRequest;
import com.vn.backend.dto.request.UpdateOrderStatusRequest;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.OrderResponse;
import com.vn.backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Order", description = "Order Management APIs")
@SecurityRequirement(name = "bearer-key")
public class OrderController {

    OrderService orderService;

    /**
     * Get all orders (Admin)
     */
    @GetMapping("/admin/all")
    @Operation(summary = "Get all orders", description = "Get all orders with pagination (Admin only)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("Admin getting all orders - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);

        ApiResponse<Page<OrderResponse>> response = ApiResponse.<Page<OrderResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Orders retrieved successfully")
                .data(orders)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's orders
     */
    @GetMapping("/my-orders")
    @Operation(summary = "Get my orders", description = "Get current user's orders with pagination")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("Getting user orders - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponse> orders = orderService.getUserOrders(pageable);

        ApiResponse<Page<OrderResponse>> response = ApiResponse.<Page<OrderResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Your orders retrieved successfully")
                .data(orders)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get orders by status (Admin)
     */
    @GetMapping("/admin/status/{status}")
    @Operation(summary = "Get orders by status", description = "Get orders filtered by status (Admin only)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("Getting orders with status: {}", status);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponse> orders = orderService.getOrdersByStatus(status.toUpperCase(), pageable);

        ApiResponse<Page<OrderResponse>> response = ApiResponse.<Page<OrderResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Orders retrieved successfully")
                .data(orders)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get order by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Get a single order by its ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        log.info("Getting order with id: {}", id);

        OrderResponse order = orderService.getOrderById(id);

        ApiResponse<OrderResponse> response = ApiResponse.<OrderResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order retrieved successfully")
                .data(order)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Create order from cart
     */
    @PostMapping
    @Operation(summary = "Create order", description = "Create a new order from current cart")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("Creating new order");

        OrderResponse order = orderService.createOrder(request);

        ApiResponse<OrderResponse> response = ApiResponse.<OrderResponse>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Order created successfully")
                .data(order)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update order status (Admin)
     */
    @PutMapping("/admin/{id}/status")
    @Operation(summary = "Update order status", description = "Update order status (Admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("Updating order {} status to: {}", id, request.getStatus());

        OrderResponse order = orderService.updateOrderStatus(id, request);

        ApiResponse<OrderResponse> response = ApiResponse.<OrderResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order status updated successfully")
                .data(order)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel order (User)
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel a pending order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long id) {
        log.info("Cancelling order with id: {}", id);

        OrderResponse order = orderService.cancelOrder(id);

        ApiResponse<OrderResponse> response = ApiResponse.<OrderResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order cancelled successfully")
                .data(order)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Delete order (Admin)
     */
    @DeleteMapping("/admin/{id}")
    @Operation(summary = "Delete order", description = "Delete an order (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        log.info("Deleting order with id: {}", id);

        orderService.deleteOrder(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order deleted successfully")
                .build();

        return ResponseEntity.ok(response);
    }
}

