package com.vn.backend.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoMoUtil {

    /**
     * Tạo signature cho MoMo sử dụng HMAC SHA256
     * 
     * @param data Chuỗi dữ liệu cần hash
     * @param secretKey Secret key từ MoMo
     * @return Signature dạng hex string
     */
    public static String hmacSHA256(String data, String secretKey) {
        try {
            Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSHA256.init(secretKeySpec);
            byte[] hash = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error creating HMAC SHA256 signature", e);
            throw new RuntimeException("Error creating signature", e);
        }
    }

    /**
     * Tạo signature cho request tạo thanh toán MoMo
     * Format: accessKey=$accessKey&amount=$amount&extraData=$extraData&ipnUrl=$ipnUrl
     *         &orderId=$orderId&orderInfo=$orderInfo&partnerCode=$partnerCode
     *         &redirectUrl=$redirectUrl&requestId=$requestId&requestType=$requestType
     * 
     * Tất cả params phải được sắp xếp theo thứ tự alphabet
     */
    public static String createSignature(String accessKey, String amount, String extraData, 
                                        String ipnUrl, String orderId, String orderInfo,
                                        String partnerCode, String redirectUrl, String requestId,
                                        String requestType, String secretKey) {
        String rawData = "accessKey=" + accessKey +
                        "&amount=" + amount +
                        "&extraData=" + extraData +
                        "&ipnUrl=" + ipnUrl +
                        "&orderId=" + orderId +
                        "&orderInfo=" + orderInfo +
                        "&partnerCode=" + partnerCode +
                        "&redirectUrl=" + redirectUrl +
                        "&requestId=" + requestId +
                        "&requestType=" + requestType;
        
        log.debug("Raw signature data: {}", rawData);
        return hmacSHA256(rawData, secretKey);
    }

    /**
     * Verify signature từ MoMo callback
     * Format cho IPN: accessKey=$accessKey&amount=$amount&extraData=$extraData&message=$message
     *                 &orderId=$orderId&orderInfo=$orderInfo&orderType=$orderType
     *                 &partnerCode=$partnerCode&payType=$payType&requestId=$requestId
     *                 &responseTime=$responseTime&resultCode=$resultCode&transId=$transId
     */
    public static boolean verifySignature(String accessKey, String amount, String extraData,
                                         String message, String orderId, String orderInfo,
                                         String orderType, String partnerCode, String payType,
                                         String requestId, String responseTime, String resultCode,
                                         String transId, String signature, String secretKey) {
        String rawData = "accessKey=" + accessKey +
                        "&amount=" + amount +
                        "&extraData=" + extraData +
                        "&message=" + message +
                        "&orderId=" + orderId +
                        "&orderInfo=" + orderInfo +
                        "&orderType=" + orderType +
                        "&partnerCode=" + partnerCode +
                        "&payType=" + payType +
                        "&requestId=" + requestId +
                        "&responseTime=" + responseTime +
                        "&resultCode=" + resultCode +
                        "&transId=" + transId;
        
        String calculatedSignature = hmacSHA256(rawData, secretKey);
        log.debug("Calculated signature: {}", calculatedSignature);
        log.debug("Received signature: {}", signature);
        
        return calculatedSignature.equals(signature);
    }

    /**
     * Generate unique request ID
     */
    public static String generateRequestId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
