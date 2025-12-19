package com.vn.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vn.backend.dto.request.PaymentRequest;
import com.vn.backend.dto.response.PaymentResponse;
import com.vn.backend.service.PaymentService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentController {

    PaymentService paymentService;

    /**
     * Lấy thông tin payment theo order ID
     * GET /api/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        log.info("GET /api/payments/order/{}", orderId);
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Xác nhận thanh toán online thành công (callback từ payment gateway)
     * POST /api/payments/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("POST /api/payments/confirm for order: {}", request.getOrderId());
        PaymentResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Xác nhận thanh toán COD khi giao hàng thành công (Admin/Shipper)
     * POST /api/payments/confirm-cod/{orderId}
     */
    @PostMapping("/confirm-cod/{orderId}")
    public ResponseEntity<PaymentResponse> confirmCODPayment(@PathVariable Long orderId) {
        log.info("POST /api/payments/confirm-cod/{}", orderId);
        PaymentResponse response = paymentService.confirmCODPayment(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Hoàn tiền (khi hủy đơn đã thanh toán) - Admin only
     * POST /api/payments/refund/{orderId}
     */
    @PostMapping("/refund/{orderId}")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable Long orderId) {
        log.info("POST /api/payments/refund/{}", orderId);
        PaymentResponse response = paymentService.refundPayment(orderId);
        return ResponseEntity.ok(response);
    }
}
