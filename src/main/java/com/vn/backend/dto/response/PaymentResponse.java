package com.vn.backend.dto.response;

import java.time.LocalDateTime;

import com.vn.backend.util.enums.PaymentMethod;
import com.vn.backend.util.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Long amount;
    private String transactionId;
    private LocalDateTime transactionTime;
    private String paymentInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
