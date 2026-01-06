package com.vn.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request callback từ VNPay về server
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayCallbackRequest {
    private String vnp_TmnCode;         // Mã website của merchant
    private String vnp_Amount;          // Số tiền thanh toán (VND * 100)
    private String vnp_BankCode;        // Mã ngân hàng
    private String vnp_BankTranNo;      // Mã giao dịch tại Ngân hàng
    private String vnp_CardType;        // Loại thẻ thanh toán
    private String vnp_OrderInfo;       // Thông tin đơn hàng
    private String vnp_PayDate;         // Thời gian thanh toán
    private String vnp_ResponseCode;    // Mã response (00 = thành công)
    private String vnp_TransactionNo;   // Mã giao dịch tại VNPAY
    private String vnp_TransactionStatus; // Trạng thái giao dịch
    private String vnp_TxnRef;          // Mã đơn hàng
    private String vnp_SecureHash;      // Hash để verify
}
