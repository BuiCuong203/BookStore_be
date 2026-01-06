package com.vn.backend.service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.vn.backend.config.VNPayConfig;
import com.vn.backend.dto.request.VNPayPaymentRequest;
import com.vn.backend.dto.response.VNPayCallbackResponse;
import com.vn.backend.dto.response.VNPayPaymentResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.Order;
import com.vn.backend.model.Payment;
import com.vn.backend.repository.OrderRepository;
import com.vn.backend.repository.PaymentRepository;
import com.vn.backend.util.VNPayUtil;
import com.vn.backend.util.enums.PaymentMethod;
import com.vn.backend.util.enums.PaymentStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý thanh toán qua VNPay
 * Kế thừa từ BasePaymentGatewayService để sử dụng RestTemplate và ObjectMapper
 */
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class VNPayService extends BasePaymentGatewayService {

    VNPayConfig vnPayConfig;
    OrderRepository orderRepository;
    PaymentRepository paymentRepository;

    public VNPayService(VNPayConfig vnPayConfig,
                        OrderRepository orderRepository,
                        PaymentRepository paymentRepository) {
        super(); // Initialize RestTemplate and ObjectMapper
        this.vnPayConfig = vnPayConfig;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        log.info("VNPayService initialized with TMN code: {}", vnPayConfig.getTmnCode());
    }

    /**
     * Tạo URL thanh toán VNPay
     */
    public VNPayPaymentResponse createPaymentUrl(VNPayPaymentRequest request, HttpServletRequest httpRequest) {
        log.info("Creating VNPay payment URL for order: {}", request.getOrderId());

        // Validate order exists
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

        // Kiểm tra order đã được thanh toán chưa
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Order has already been paid");
        }

        try {
            // Tạo các tham số cho VNPay
            String vnp_Version = vnPayConfig.getVersion();
            String vnp_Command = vnPayConfig.getCommand();
            String vnp_TmnCode = vnPayConfig.getTmnCode();
            String orderType = vnPayConfig.getOrderType();
            
            // Lấy amount từ order và nhân 100 theo yêu cầu của VNPay
            long amount = order.getTotalAmount() * 100;
            
            // Tạo mã giao dịch unique
            String vnp_TxnRef = "ORDER" + request.getOrderId() + "_" + System.currentTimeMillis();
            String vnp_IpAddr = VNPayUtil.getIpAddress(httpRequest);
            
            // Thông tin đơn hàng
            String vnp_OrderInfo = request.getOrderInfo() != null 
                ? request.getOrderInfo() 
                : "Thanh toan don hang " + request.getOrderId();

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.put("vnp_OrderType", orderType);
            vnp_Params.put("vnp_Locale", "vn");
            
            // Return URL - nơi VNPay redirect về sau khi thanh toán
            String returnUrl = request.getReturnUrl() != null 
                ? request.getReturnUrl() 
                : vnPayConfig.getReturnUrl();
            vnp_Params.put("vnp_ReturnUrl", returnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

            // Thời gian tạo và hết hạn
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            
            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            // Build hash data & query string
            String hashData = VNPayUtil.buildHashData(vnp_Params);
            String vnp_SecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
            String queryUrl = VNPayUtil.buildQuery(vnp_Params) + "&vnp_SecureHash=" + vnp_SecureHash;
            
            String paymentUrl = vnPayConfig.getVnpUrl() + "?" + queryUrl;

            log.info("VNPay payment URL created: {}", paymentUrl);

            // Tạo Payment record với status PENDING
            Payment payment = Payment.builder()
                    .order(order)
                    .paymentMethod(PaymentMethod.VNPAY)
                    .amount(order.getTotalAmount())
                    .paymentStatus(PaymentStatus.PENDING)
                    .paymentInfo("VNPay - " + vnp_OrderInfo)
                    .build();
            paymentRepository.save(payment);
            
            return VNPayPaymentResponse.builder()
                    .paymentUrl(paymentUrl)
                    .txnRef(vnp_TxnRef)
                    .message("Create payment URL successfully")
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating VNPay payment URL", e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                "Error creating payment URL: " + e.getMessage());
        }
    }

    /**
     * Xử lý callback từ VNPay sau khi thanh toán
     */
    @Transactional
    public VNPayCallbackResponse handleCallback(Map<String, String> params) {
        log.info("Processing VNPay callback with {} params", params.size());
        log.debug("VNPay callback params: {}", params);

        try {
            // Lấy secure hash từ params
            String vnp_SecureHash = params.get("vnp_SecureHash");
            
            // Kiểm tra vnp_SecureHash có tồn tại không
            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                log.error("vnp_SecureHash is null or empty! Params received: {}", params.keySet());
                log.error("All params: {}", params);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "Missing vnp_SecureHash in callback");
            }
            
            log.info("Received vnp_SecureHash: {}", vnp_SecureHash);
            
            // Remove hash params để verify
            Map<String, String> verifyParams = new HashMap<>(params);
            verifyParams.remove("vnp_SecureHash");
            verifyParams.remove("vnp_SecureHashType");

            // Tạo hash từ params để verify
            String signValue = VNPayUtil.hashAllFields(verifyParams);
            String checkHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), signValue);

            log.info("Hash verification - Expected: {}, Received: {}", checkHash, vnp_SecureHash);

            // Verify hash
            if (!checkHash.equals(vnp_SecureHash)) {
                log.error("Invalid secure hash. Expected: {}, Received: {}", checkHash, vnp_SecureHash);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid secure hash");
            }

            // Parse params
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_TransactionNo = params.get("vnp_TransactionNo");
            String vnp_TxnRef = params.get("vnp_TxnRef");
            String vnp_Amount = params.get("vnp_Amount");
            String vnp_BankCode = params.get("vnp_BankCode");
            String vnp_PayDate = params.get("vnp_PayDate");
            String vnp_OrderInfo = params.get("vnp_OrderInfo");

            // Parse order ID
            Long orderId = parseOrderIdFromTxnRef(vnp_TxnRef);
            
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

            // Kiểm tra xem đã xử lý callback này chưa (idempotency check)
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                log.warn("Order {} already paid, skipping duplicate callback", orderId);
                return VNPayCallbackResponse.builder()
                        .status("SUCCESS")
                        .message("Payment already confirmed")
                        .orderId(orderId)
                        .transactionNo(vnp_TransactionNo)
                        .amount(Long.parseLong(vnp_Amount) / 100)
                        .bankCode(vnp_BankCode)
                        .payDate(vnp_PayDate)
                        .build();
            }

            // Validate amount
            long callbackAmount = Long.parseLong(vnp_Amount) / 100;
            if (callbackAmount != order.getTotalAmount()) {
                log.error("Amount mismatch. Expected: {}, Received: {}", 
                    order.getTotalAmount(), callbackAmount);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                    "Payment amount does not match order amount");
            }

            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElse(Payment.builder()
                            .order(order)
                            .paymentMethod(PaymentMethod.VNPAY)
                            .build());

            // Response code 00 = thành công
            if ("00".equals(vnp_ResponseCode)) {
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setTransactionId(vnp_TransactionNo);
                payment.setAmount(callbackAmount);
                payment.setTransactionTime(parseVNPayDate(vnp_PayDate));
                payment.setPaymentInfo("VNPay - " + vnp_BankCode + " - " + vnp_OrderInfo);
                paymentRepository.save(payment);

                order.setPaymentStatus(PaymentStatus.PAID);
                orderRepository.save(order);

                log.info("Payment successful for order: {}", orderId);

                return VNPayCallbackResponse.builder()
                        .status("SUCCESS")
                        .message("Payment successful")
                        .orderId(orderId)
                        .transactionNo(vnp_TransactionNo)
                        .amount(callbackAmount)
                        .bankCode(vnp_BankCode)
                        .payDate(vnp_PayDate)
                        .build();
            } else {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setAmount(callbackAmount);
                payment.setTransactionTime(parseVNPayDate(vnp_PayDate));
                payment.setPaymentInfo("VNPay failed - Code: " + vnp_ResponseCode);
                paymentRepository.save(payment);

                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);

                log.warn("Payment failed for order: {} with code: {}", orderId, vnp_ResponseCode);

                return VNPayCallbackResponse.builder()
                        .status("FAILED")
                        .message(getVNPayResponseMessage(vnp_ResponseCode))
                        .orderId(orderId)
                        .transactionNo(vnp_TransactionNo)
                        .amount(callbackAmount)
                        .bankCode(vnp_BankCode)
                        .payDate(vnp_PayDate)
                        .build();
            }

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing VNPay callback", e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                "Error processing callback: " + e.getMessage());
        }
    }

    /**
     * Parse order ID từ txnRef
     * Format: ORDER{orderId}_{timestamp}
     */
    private Long parseOrderIdFromTxnRef(String txnRef) {
        try {
            String orderIdStr = txnRef.substring(5, txnRef.indexOf("_")); // Lấy phần sau "ORDER" và trước "_"
            return Long.parseLong(orderIdStr);
        } catch (Exception e) {
            log.error("Error parsing order ID from txnRef: {}", txnRef, e);
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid transaction reference");
        }
    }

    /**
     * Parse date từ VNPay format (yyyyMMddHHmmss) sang LocalDateTime
     */
    private LocalDateTime parseVNPayDate(String vnpayDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(vnpayDate, formatter);
        } catch (Exception e) {
            log.error("Error parsing VNPay date: {}", vnpayDate, e);
            return LocalDateTime.now();
        }
    }

    private String getVNPayResponseMessage(String responseCode) {
        Map<String, String> messages = new HashMap<>();
        messages.put("00", "Giao dịch thành công");
        messages.put("07", "Trừ tiền thành công. Giao dịch bị nghi ngờ");
        messages.put("09", "Giao dịch không thành công do thẻ chưa đăng ký dịch vụ");
        messages.put("10", "Giao dịch không thành công do xác thực không đúng");
        messages.put("11", "Giao dịch không thành công do đã hết hạn chờ thanh toán");
        messages.put("12", "Giao dịch không thành công do thẻ bị khóa");
        messages.put("24", "Giao dịch không thành công do khách hàng hủy");
        messages.put("51", "Giao dịch không thành công do tài khoản không đủ số dư");
        messages.put("65", "Giao dịch không thành công do vượt quá số lần nhập");
        messages.put("75", "Ngân hàng thanh toán đang bảo trì");
        messages.put("79", "Giao dịch không thành công do nhập sai mật khẩu quá số lần");
        messages.put("99", "Lỗi không xác định");
        
        return messages.getOrDefault(responseCode, "Giao dịch thất bại - Mã lỗi: " + responseCode);
    }
}
