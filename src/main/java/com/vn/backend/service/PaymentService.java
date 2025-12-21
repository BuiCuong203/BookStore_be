package com.vn.backend.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.vn.backend.dto.request.PaymentRequest;
import com.vn.backend.dto.response.PaymentResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.Order;
import com.vn.backend.model.Payment;
import com.vn.backend.repository.OrderRepository;
import com.vn.backend.repository.PaymentRepository;
import com.vn.backend.util.enums.PaymentMethod;
import com.vn.backend.util.enums.PaymentStatus;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentService {

    PaymentRepository paymentRepository;
    OrderRepository orderRepository;

    /**
     * Chuyển Payment sang PaymentResponse
     */
    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .transactionTime(payment.getTransactionTime())
                .paymentInfo(payment.getPaymentInfo())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    /**
     * Tạo payment record khi tạo đơn hàng
     */
    @Transactional
    public PaymentResponse createPayment(Order order) {
        log.info("Creating payment for order: {}", order.getId());

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(order.getMethodPayment())
                .paymentStatus(order.getPaymentStatus())
                .amount(order.getTotalAmount())
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment created with id: {}", payment.getId());

        return toPaymentResponse(payment);
    }

    /**
     * Lấy thông tin payment theo order ID
     */
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        log.info("Getting payment for order: {}", orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), 
                    "Payment not found for order: " + orderId));

        return toPaymentResponse(payment);
    }

    /**
     * Xác nhận thanh toán thành công (cho online payment)
     */
    @Transactional
    public PaymentResponse confirmPayment(PaymentRequest request) {
        log.info("Confirming payment for order: {}", request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Payment not found"));

        // Cập nhật trạng thái payment
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setTransactionId(request.getTransactionId());
        payment.setTransactionTime(LocalDateTime.now());
        payment.setPaymentInfo(request.getPaymentInfo());
        payment = paymentRepository.save(payment);

        // Cập nhật trạng thái payment trong order
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        log.info("Payment confirmed successfully for order: {}", request.getOrderId());
        return toPaymentResponse(payment);
    }

    /**
     * Đánh dấu thanh toán thất bại
     */
    @Transactional
    public PaymentResponse failPayment(Long orderId, String reason) {
        log.info("Marking payment as failed for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Payment not found"));

        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setPaymentInfo(reason);
        payment = paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(order);

        log.info("Payment marked as failed for order: {}", orderId);
        return toPaymentResponse(payment);
    }

    /**
     * Hoàn tiền (khi hủy đơn đã thanh toán)
     */
    @Transactional
    public PaymentResponse refundPayment(Long orderId) {
        log.info("Refunding payment for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Payment not found"));

        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                "Can only refund paid payments");
        }

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        payment.setPaymentInfo("Refunded due to order cancellation");
        payment = paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(order);

        log.info("Payment refunded for order: {}", orderId);
        return toPaymentResponse(payment);
    }

    /**
     * Xác nhận thanh toán COD khi giao hàng thành công
     */
    @Transactional
    public PaymentResponse confirmCODPayment(Long orderId) {
        log.info("Confirming COD payment for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order not found"));

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Payment not found"));

        if (payment.getPaymentMethod() != PaymentMethod.COD) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), 
                "This is not a COD payment");
        }

        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setTransactionTime(LocalDateTime.now());
        payment.setPaymentInfo("Cash collected on delivery");
        payment = paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        log.info("COD payment confirmed for order: {}", orderId);
        return toPaymentResponse(payment);
    }
}
