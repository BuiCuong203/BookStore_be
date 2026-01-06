package com.vn.backend.service;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/**
 * Base service class cho các payment gateway services
 * Cung cấp các tiện ích chung như RestTemplate và ObjectMapper
 */
@Slf4j
public abstract class BasePaymentGatewayService {

    protected RestTemplate restTemplate;
    protected ObjectMapper objectMapper;

    protected BasePaymentGatewayService() {
        this.restTemplate = createRestTemplate();
        this.objectMapper = createObjectMapper();
    }

    /**
     * Tạo RestTemplate với cấu hình timeout
     */
    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 seconds
        factory.setReadTimeout(30000);    // 30 seconds
        log.debug("Creating RestTemplate with 30s timeout");
        return new RestTemplate(factory);
    }

    /**
     * Tạo ObjectMapper với support cho Java 8 Date/Time API
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        log.debug("Creating ObjectMapper with JavaTimeModule registered");
        return mapper;
    }

    /**
     * Cho phép custom RestTemplate nếu cần
     */
    protected void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Cho phép custom ObjectMapper nếu cần
     */
    protected void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
