package com.vn.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để tạo payment URL VNPay
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayPaymentRequest {
    private Long orderId;        // ID của đơn hàng
    private String orderInfo;    // Thông tin đơn hàng (optional)
    private String returnUrl;    // URL return sau khi thanh toán (optional, có thể dùng default config)
}
