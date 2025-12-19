package com.vn.backend.util.enums;

/**
 * Enum định nghĩa trạng thái thanh toán
 */
public enum PaymentStatus {
    UNPAID,     // Chưa thanh toán
    PAID,       // Đã thanh toán
    REFUNDED,   // Đã hoàn tiền
    FAILED      // Thanh toán thất bại
}
