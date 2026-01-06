package com.vn.backend.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VNPayUtil {

    /**
     * Tạo secure hash cho VNPay
     */
    public static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("Error when create secure hash", ex);
            return "";
        }
    }

    /**
     * Tạo hash data theo yêu cầu VNPay: fieldName=URLEncode(fieldValue), nối bằng '&'
     */
    public static String buildHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        List<String> pairs = new ArrayList<>();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    pairs.add(fieldName + "=" + URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (UnsupportedEncodingException e) {
                    log.error("Error encoding hash data", e);
                }
            }
        }
        return String.join("&", pairs);
    }

    /**
     * Tạo query string từ map params (URLEncode cả key và value)
     */
    public static String buildQuery(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        List<String> encodedPairs = new ArrayList<>();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    encodedPairs.add(
                        URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()) + "=" +
                        URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString())
                    );
                } catch (UnsupportedEncodingException e) {
                    log.error("Error encoding query", e);
                }
            }
        }
        return String.join("&", encodedPairs);
    }

    /**
     * Tạo transaction number (dạng: yyyyMMddHHmmss)
     */
    public static String getRandomNumber(int len) {
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    /**
     * Format date theo định dạng VNPay yêu cầu
     */
    public static String getVNPayDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.format(date);
    }

    /**
     * Tạo IP Address từ request
     */
    public static String getIpAddress(jakarta.servlet.http.HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        return ipAdress;
    }

    /**
     * Parse query string sang Map
     */
    public static Map<String, String> getParamsFromQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();
        if (queryString != null && !queryString.isEmpty()) {
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0 && idx < pair.length() - 1) {
                    String key = pair.substring(0, idx);
                    String value = pair.substring(idx + 1);
                    params.put(key, value);
                }
            }
        }
        return params;
    }

    /**
     * Tạo hash data để verify
     */
    public static String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);

            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Logic thêm dấu & chuẩn: Chỉ thêm nếu sb đã có dữ liệu trước đó
                if (sb.length() > 0) {
                    sb.append("&");
                }

                sb.append(fieldName);
                sb.append("=");

                // --- SỬA QUAN TRỌNG: ENCODE VALUE ---
                try {
                    // Encode dữ liệu sang UTF-8 (ví dụ: dấu cách thành + hoặc %20)
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }
}
