package com.vn.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vn.backend.dto.request.VNPayPaymentRequest;
import com.vn.backend.dto.response.VNPayCallbackResponse;
import com.vn.backend.dto.response.VNPayPaymentResponse;
import com.vn.backend.service.VNPayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payments/vnpay")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "VNPay Payment", description = "VNPay payment gateway integration APIs")
public class VNPayPaymentController {

    VNPayService vnPayService;

    /**
     * Tạo payment URL cho VNPay
     * POST /api/payments/vnpay/create
     */
    @PostMapping("/create")
    @Operation(summary = "Create VNPay payment URL", description = "Generate VNPay payment URL for an order")
    public ResponseEntity<VNPayPaymentResponse> createPaymentUrl(
            @Valid @RequestBody VNPayPaymentRequest request,
            HttpServletRequest httpRequest) {
        log.info("Creating VNPay payment for order: {}", request.getOrderId());
        VNPayPaymentResponse response = vnPayService.createPaymentUrl(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Xử lý callback từ VNPay (IPN - Instant Payment Notification)
     * GET /api/payments/vnpay/notify
     */
    @GetMapping("/notify")
    @Operation(summary = "VNPay IPN callback", description = "Handle callback from VNPay payment gateway (server-to-server)")
    public ResponseEntity<VNPayCallbackResponse> handleNotify(@RequestParam Map<String, String> params) {
        log.info("Received VNPay IPN notification");
        log.info("VNPay IPN params count: {}", params.size());
        
        VNPayCallbackResponse response = vnPayService.handleCallback(params);
        return ResponseEntity.ok(response);
    }

    /**
     * Xử lý return URL từ VNPay (sau khi user thanh toán xong)
     * GET /api/payments/vnpay/callback
     */
    @GetMapping("/callback")
    @Operation(summary = "VNPay return URL", description = "Handle redirect from VNPay after user completes payment")
    public ResponseEntity<VNPayCallbackResponse> handleCallback(@RequestParam Map<String, String> params) {
        log.info("Received VNPay callback from user");
        log.info("VNPay callback params count: {}", params.size());
        
        VNPayCallbackResponse response = vnPayService.handleCallback(params);
        return ResponseEntity.ok(response);
    }
}
