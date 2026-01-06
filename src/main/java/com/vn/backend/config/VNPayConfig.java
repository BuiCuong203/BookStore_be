package com.vn.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class VNPayConfig {
    
    @Value("${vnpay.tmn-code}")
    private String tmnCode;
    
    @Value("${vnpay.hash-secret}")
    private String hashSecret;
    
    @Value("${vnpay.url}")
    private String vnpUrl;
    
    @Value("${vnpay.return-url}")
    private String returnUrl;
    
    @Value("${vnpay.version}")
    private String version;
    
    @Value("${vnpay.command}")
    private String command;
    
    @Value("${vnpay.order-type}")
    private String orderType;
}
