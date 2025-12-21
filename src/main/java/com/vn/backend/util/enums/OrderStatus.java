package com.vn.backend.util.enums;

/**
 * Enum định nghĩa các trạng thái đơn hàng
 * Luồng  PENDING → CONFIRMED → PROCESSING → SHIPPING → DELIVERED
 * Hoặc: PENDING → CANCELLED
 */
public enum OrderStatus {
    PENDING,      // Đơn hàng mới tạo, chờ xác nhận
    CONFIRMED,    // Admin đã xác nhận đơn hàng
    PROCESSING,   // Đang chuẩn bị hàng
    SHIPPING,     // Đang giao hàng
    DELIVERED,    // Đã giao hàng thành công
    CANCELLED     // Đơn hàng đã bị hủy
}
