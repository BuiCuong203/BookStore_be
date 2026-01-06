package com.vn.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response sau khi xử lý callback từ MoMo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoMoCallbackResponse {
    private Integer resultCode;     // Mã kết quả (0 = success)
    private String message;         // Thông báo
    private String orderId;         // Order ID
    private Long amount;            // Số tiền
    private String transId;         // MoMo transaction ID
    private String orderInfo;       // Thông tin đơn hàng
    private String paymentStatus;   // Trạng thái thanh toán (PAID/FAILED)
}
