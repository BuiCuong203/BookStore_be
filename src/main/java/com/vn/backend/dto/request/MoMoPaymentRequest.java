package com.vn.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để tạo payment URL MoMo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoMoPaymentRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;        // ID của đơn hàng
    
    private String orderInfo;    // Thông tin đơn hàng (optional)
    private String returnUrl;    // URL return sau khi thanh toán (optional)
    private String notifyUrl;    // URL IPN callback (optional)
}
