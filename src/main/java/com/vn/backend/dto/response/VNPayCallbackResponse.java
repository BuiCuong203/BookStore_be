package com.vn.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response sau khi xử lý callback từ VNPay
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayCallbackResponse {
    private String status;          // SUCCESS, FAILED
    private String message;         // Thông báo
    private Long orderId;           // ID đơn hàng
    private String transactionNo;   // Mã giao dịch VNPay
    private Long amount;            // Số tiền đã thanh toán
    private String bankCode;        // Mã ngân hàng
    private String payDate;         // Thời gian thanh toán
}
