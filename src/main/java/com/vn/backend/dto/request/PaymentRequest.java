package com.vn.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    private String transactionId;
    private String paymentInfo;
}
