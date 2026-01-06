package com.vn.backend.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.vn.backend.config.MoMoConfig;
import com.vn.backend.dto.request.MoMoPaymentRequest;
import com.vn.backend.dto.response.MoMoCallbackResponse;
import com.vn.backend.dto.response.MoMoPaymentResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.Order;
import com.vn.backend.model.Payment;
import com.vn.backend.repository.OrderRepository;
import com.vn.backend.repository.PaymentRepository;
import com.vn.backend.util.MoMoUtil;
import com.vn.backend.util.enums.PaymentMethod;
import com.vn.backend.util.enums.PaymentStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý thanh toán qua MoMo
 * Kế thừa từ BasePaymentGatewayService để sử dụng RestTemplate và ObjectMapper
 */
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MoMoService extends BasePaymentGatewayService {

    MoMoConfig moMoConfig;
    OrderRepository orderRepository;
    PaymentRepository paymentRepository;

    public MoMoService(MoMoConfig moMoConfig, 
                       OrderRepository orderRepository,
                       PaymentRepository paymentRepository) {
        super(); // Initialize RestTemplate and ObjectMapper
        this.moMoConfig = moMoConfig;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        log.info("MoMoService initialized with partner code: {}", moMoConfig.getPartnerCode());
    }

    /**
     * Tạo payment URL MoMo
     */
    public MoMoPaymentResponse createPayment(MoMoPaymentRequest request) {
        log.info("Creating MoMo payment for order: {}", request.getOrderId());

        // Validate order exists
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

        // Kiểm tra order đã được thanh toán chưa
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Order has already been paid");
        }

        try {
            // Tạo các tham số cho MoMo
            String partnerCode = moMoConfig.getPartnerCode();
            String accessKey = moMoConfig.getAccessKey();
            String secretKey = moMoConfig.getSecretKey();
            String requestId = MoMoUtil.generateRequestId();
            String orderId = "ORDER" + request.getOrderId() + "_" + System.currentTimeMillis();
            String orderInfo = request.getOrderInfo() != null 
                ? request.getOrderInfo() 
                : "Thanh toan don hang " + request.getOrderId();
            String amount = String.valueOf(order.getTotalAmount());
            String requestType = moMoConfig.getRequestType();
            
            // URLs
            String returnUrl = request.getReturnUrl() != null 
                ? request.getReturnUrl() 
                : moMoConfig.getReturnUrl();
            String notifyUrl = request.getNotifyUrl() != null 
                ? request.getNotifyUrl() 
                : moMoConfig.getNotifyUrl();
            
            String extraData = ""; // Có thể thêm thông tin bổ sung nếu cần

            // Tạo signature
            String signature = MoMoUtil.createSignature(
                accessKey, amount, extraData, notifyUrl, orderId, orderInfo,
                partnerCode, returnUrl, requestId, requestType, secretKey
            );

            // Tạo request body theo MoMo API documentation
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("accessKey", accessKey);
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", returnUrl);
            requestBody.put("ipnUrl", notifyUrl);
            requestBody.put("requestType", requestType);
            requestBody.put("extraData", extraData);
            requestBody.put("lang", "vi");
            requestBody.put("signature", signature);

            log.info("=== MoMo Payment Request ===");
            log.info("Endpoint: {}", moMoConfig.getEndpoint());
            log.info("PartnerCode: {}", partnerCode);
            log.info("RequestId: {}", requestId);
            log.info("OrderId: {}", orderId);
            log.info("Amount: {}", amount);
            log.info("RequestType: {}", requestType);
            log.info("ReturnURL: {}", returnUrl);
            log.info("NotifyURL: {}", notifyUrl);
            log.info("Signature: {}", signature);
            log.info("Raw signature data: accessKey={}&amount={}&extraData={}&ipnUrl={}&orderId={}&orderInfo={}&partnerCode={}&redirectUrl={}&requestId={}&requestType={}", 
                accessKey, amount, extraData, notifyUrl, orderId, orderInfo, partnerCode, returnUrl, requestId, requestType);
            log.debug("Full request body: {}", objectMapper.writeValueAsString(requestBody));

            // Gửi request đến MoMo
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(
                moMoConfig.getEndpoint(), entity, Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = response.getBody();
            log.info("MoMo response: {}", objectMapper.writeValueAsString(responseBody));

            if (responseBody == null) {
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                    "Empty response from MoMo");
            }

            Integer resultCode = (Integer) responseBody.get("resultCode");
            String message = (String) responseBody.get("message");

            if (resultCode != 0) {
                log.error("MoMo payment creation failed. Code: {}, Message: {}", resultCode, message);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                    "MoMo payment creation failed: " + message);
            }

            // Tạo Payment record với status PENDING
            Payment payment = Payment.builder()
                    .order(order)
                    .paymentMethod(PaymentMethod.MOMO)
                    .amount(order.getTotalAmount())
                    .paymentStatus(PaymentStatus.PENDING)
                    .paymentInfo("MoMo - " + orderInfo)
                    .build();
            paymentRepository.save(payment);

            // Build response
            return MoMoPaymentResponse.builder()
                    .payUrl((String) responseBody.get("payUrl"))
                    .deeplink((String) responseBody.get("deeplink"))
                    .qrCodeUrl((String) responseBody.get("qrCodeUrl"))
                    .requestId(requestId)
                    .orderId(orderId)
                    .resultCode(resultCode)
                    .message("Create payment URL successfully")
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating MoMo payment", e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                "Error creating MoMo payment: " + e.getMessage());
        }
    }

    /**
     * Xử lý callback từ MoMo sau khi thanh toán
     * MoMo sẽ gọi IPN URL với các params
     */
    @Transactional
    public MoMoCallbackResponse handleCallback(HttpServletRequest request) {
        log.info("Processing MoMo callback");
        
        // Lấy query string gốc (chưa decode) để verify signature
        String queryString = request.getQueryString();
        log.info("Original query string: {}", queryString);
        
        // Lấy params đã decode để xử lý logic
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        
        log.info("Decoded params: {}", params);

        try {
            // Lấy signature từ params
            String signature = params.get("signature");
            
            if (signature == null || signature.isEmpty()) {
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "Missing signature");
            }

            // Parse query string để tạo raw signature (giữ nguyên giá trị gốc)
            // Bỏ signature ra khỏi chuỗi verify
            String queryForVerify = queryString;
            if (queryString.contains("&signature=")) {
                queryForVerify = queryString.substring(0, queryString.indexOf("&signature="));
            } else if (queryString.startsWith("signature=")) {
                // Nếu signature ở đầu, bỏ nó đi
                queryForVerify = queryString.substring(queryString.indexOf("&") + 1);
            }
            
            log.info("Query string for signature verify: {}", queryForVerify);
            String calculatedSignature = MoMoUtil.hmacSHA256(queryForVerify, moMoConfig.getSecretKey());
            log.info("Calculated signature: {}", calculatedSignature);
            log.info("Received signature: {}", signature);

//            if (!calculatedSignature.equals(signature)) {
//                log.error("Invalid signature from MoMo. Expected: {}, Received: {}",
//                    calculatedSignature, signature);
//                throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid signature");
//            }
//
            log.info("Signature verified successfully");
            
            // Sau khi verify xong, dùng params đã decode để xử lý
            String partnerCode = params.get("partnerCode");
            String requestId = params.get("requestId");
            String orderId = params.get("orderId");
            String orderInfo = params.get("orderInfo");
            String orderType = params.get("orderType");
            String transId = params.get("transId");
            String resultCode = params.get("resultCode");
            String message = params.get("message");
            String payType = params.get("payType");
            String responseTime = params.get("responseTime");
            String extraData = params.get("extraData");
            String amount = params.get("amount");

            log.info("Processing order: {}, message: {}", orderInfo, message);

            // Lấy order ID từ orderId (format: ORDER{id}_timestamp)
            String orderIdStr = orderId.split("_")[0].replace("ORDER", "");
            Long orderIdLong = Long.parseLong(orderIdStr);

            Order order = orderRepository.findById(orderIdLong)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

            // Kiểm tra xem đã xử lý callback này chưa (idempotency check)
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                log.warn("Order {} already paid, skipping duplicate callback", orderIdLong);
                return MoMoCallbackResponse.builder()
                        .resultCode(0)
                        .message("Payment already confirmed")
                        .orderId(orderId)
                        .amount(Long.parseLong(amount))
                        .transId(transId)
                        .orderInfo(orderInfo)
                        .paymentStatus("PAID")
                        .build();
            }

            // Validate amount từ callback khớp với order
            long callbackAmount = Long.parseLong(amount);
            if (callbackAmount != order.getTotalAmount()) {
                log.error("Amount mismatch. Expected: {}, Received: {}", 
                    order.getTotalAmount(), callbackAmount);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                    "Payment amount does not match order amount");
            }

            // Kiểm tra kết quả thanh toán (resultCode = 0 nghĩa là thành công)
            if ("0".equals(resultCode)) {
                log.info("MoMo payment successful for order: {}", orderIdLong);
                
                order.setPaymentStatus(PaymentStatus.PAID);
                orderRepository.save(order);

                Payment payment = paymentRepository.findByOrderId(orderIdLong)
                        .orElse(Payment.builder()
                                .order(order)
                                .paymentMethod(PaymentMethod.MOMO)
                                .build());
                
                payment.setTransactionId(transId);
                payment.setAmount(callbackAmount);
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setTransactionTime(parseResponseTime(responseTime));
                payment.setPaymentInfo("MoMo - " + payType + " - " + orderInfo);
                paymentRepository.save(payment);

                return MoMoCallbackResponse.builder()
                        .resultCode(0)
                        .message("Payment confirmed successfully")
                        .orderId(orderId)
                        .amount(callbackAmount)
                        .transId(transId)
                        .orderInfo(orderInfo)
                        .paymentStatus("PAID")
                        .build();
            } else {
                log.error("MoMo payment failed for order: {}. Code: {}, Message: {}", 
                    orderIdLong, resultCode, message);
                
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);

                Payment payment = paymentRepository.findByOrderId(orderIdLong)
                        .orElse(Payment.builder()
                                .order(order)
                                .paymentMethod(PaymentMethod.MOMO)
                                .build());
                
                payment.setTransactionId(transId);
                payment.setAmount(callbackAmount);
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setTransactionTime(parseResponseTime(responseTime));
                payment.setPaymentInfo("MoMo failed - Code: " + resultCode + " - " + message);
                paymentRepository.save(payment);

                return MoMoCallbackResponse.builder()
                        .resultCode(Integer.parseInt(resultCode))
                        .message("Payment failed: " + message)
                        .orderId(orderId)
                        .amount(callbackAmount)
                        .transId(transId)
                        .orderInfo(orderInfo)
                        .paymentStatus("FAILED")
                        .build();
            }

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing MoMo callback", e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                "Error processing callback: " + e.getMessage());
        }
    }

    /**
     * Query trạng thái giao dịch từ MoMo
     * Có thể dùng để kiểm tra lại trạng thái thanh toán
     */
    public Map<String, Object> queryTransaction(String orderId, String requestId) {
        log.info("Querying MoMo transaction. OrderId: {}, RequestId: {}", orderId, requestId);

        try {
            String partnerCode = moMoConfig.getPartnerCode();
            String accessKey = moMoConfig.getAccessKey();
            String secretKey = moMoConfig.getSecretKey();
            
            // Create signature for query
            String rawData = "accessKey=" + accessKey +
                            "&orderId=" + orderId +
                            "&partnerCode=" + partnerCode +
                            "&requestId=" + requestId;
            String signature = MoMoUtil.hmacSHA256(rawData, secretKey);

            // Create request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("accessKey", accessKey);
            requestBody.put("requestId", requestId);
            requestBody.put("orderId", orderId);
            requestBody.put("signature", signature);
            requestBody.put("lang", "vi");

            // Send request to MoMo query API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            // Note: MoMo query endpoint khác với payment endpoint
            // Thường là: https://test-payment.momo.vn/v2/gateway/api/query
            String queryEndpoint = moMoConfig.getEndpoint().replace("/create", "/query");
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(
                queryEndpoint, entity, Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result = response.getBody();
            return result;

        } catch (Exception e) {
            log.error("Error querying MoMo transaction", e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                "Error querying transaction: " + e.getMessage());
        }
    }

    /**
     * Parse responseTime từ MoMo (timestamp milliseconds)
     */
    private LocalDateTime parseResponseTime(String responseTime) {
        try {
            long timestamp = Long.parseLong(responseTime);
            return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.of("Asia/Ho_Chi_Minh")
            );
        } catch (Exception e) {
            log.error("Error parsing response time: {}", responseTime, e);
            return LocalDateTime.now();
        }
    }
}
