package com.vn.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateCouponRequest {
    private String description;

    @Min(value = 0, message = "Stock quantity must be at least 0")
    private Integer stockQuantity;

    @Pattern(regexp = "^(PERCENTAGE|FIXED)$", message = "Discount type must be PERCENTAGE or FIXED")
    private String discountType;

    @Min(value = 1, message = "Discount value must be at least 1")
    private Integer discount;
}

