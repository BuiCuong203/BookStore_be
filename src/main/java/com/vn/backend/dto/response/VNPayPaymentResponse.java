package com.vn.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response khi tạo payment URL thành công
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayPaymentResponse {
    private String paymentUrl;      // URL thanh toán VNPay
    private String txnRef;          // Mã tham chiếu giao dịch
    private String message;         // Thông báo
}
