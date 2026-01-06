package com.vn.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vn.backend.dto.request.MoMoPaymentRequest;
import com.vn.backend.dto.response.MoMoCallbackResponse;
import com.vn.backend.dto.response.MoMoPaymentResponse;
import com.vn.backend.service.MoMoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payments/momo")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "MoMo Payment", description = "MoMo payment gateway integration APIs")
public class MoMoPaymentController {

    MoMoService moMoService;

    /**
     * Tạo payment URL cho MoMo
     * POST /api/payments/momo/create
     */
    @PostMapping("/create")
    @Operation(summary = "Create MoMo payment URL", description = "Generate MoMo payment URL for an order")
    public ResponseEntity<MoMoPaymentResponse> createPayment(@Valid @RequestBody MoMoPaymentRequest request) {
        log.info("Creating MoMo payment for order: {}", request.getOrderId());
        MoMoPaymentResponse response = moMoService.createPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Xử lý callback từ MoMo (IPN - Instant Payment Notification)
     * POST /api/payments/momo/notify
     */
    @PostMapping("/notify")
    @Operation(summary = "MoMo IPN callback", description = "Handle callback from MoMo payment gateway (server-to-server)")
    public ResponseEntity<MoMoCallbackResponse> handleNotify(HttpServletRequest request) {
        log.info("Received MoMo IPN notification");
        MoMoCallbackResponse response = moMoService.handleCallback(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Xử lý return URL từ MoMo (sau khi user thanh toán xong)
     * GET /api/payments/momo/callback
     */
    @GetMapping("/callback")
    @Operation(summary = "MoMo return URL", description = "Handle redirect from MoMo after user completes payment")
    public ResponseEntity<MoMoCallbackResponse> handleCallback(HttpServletRequest request) {
        log.info("Received MoMo callback from user");
        MoMoCallbackResponse response = moMoService.handleCallback(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Query trạng thái giao dịch từ MoMo
     * GET /api/payments/momo/query
     */
    @GetMapping("/query")
    @Operation(summary = "Query MoMo transaction", description = "Query transaction status from MoMo")
    public ResponseEntity<Map<String, Object>> queryTransaction(
            @RequestParam String orderId,
            @RequestParam String requestId) {
        log.info("Querying MoMo transaction: orderId={}, requestId={}", orderId, requestId);
        Map<String, Object> response = moMoService.queryTransaction(orderId, requestId);
        return ResponseEntity.ok(response);
    }
}
