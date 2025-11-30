package com.vn.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CouponResponse {
    private Long id;
    private String code;
    private String description;
    private Integer stockQuantity;
    private String discountType;
    private Integer discount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

