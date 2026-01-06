package com.vn.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response sau khi tạo payment URL MoMo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoMoPaymentResponse {
    private String payUrl;          // URL để redirect người dùng đến trang thanh toán MoMo
    private String deeplink;        // Deep link để mở MoMo app (cho mobile)
    private String qrCodeUrl;       // URL QR code để quét MoMo
    private String requestId;       // ID request duy nhất
    private String orderId;         // Order ID
    private Integer resultCode;     // Mã kết quả từ MoMo (0 = success)
    private String message;         // Thông báo
}
