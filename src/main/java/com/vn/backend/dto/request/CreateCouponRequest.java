package com.vn.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateCouponRequest {
    @NotBlank(message = "Cần có mã giảm giá.")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Mã giảm giá chỉ được chứa chữ cái viết hoa, số, dấu gạch dưới và dấu gạch ngang.")
    private String code;

    private String description;

    @NotNull(message = "Cần có số lượng hàng tồn kho")
    @Min(value = 1, message = "Số lượng hàng tồn kho tối thiểu phải là 1.")
    private Integer stockQuantity;

    @NotBlank(message = "Loại giảm giá là bắt buộc")
    @Pattern(regexp = "^(PERCENTAGE|FIXED)$", message = "Loại chiết khấu phải là PHẦN TRĂM hoặc CỐ ĐỊNH.")
    private String discountType;

    @NotNull(message = "Giá trị chiết khấu là bắt buộc")
    @Min(value = 1, message = "Giá trị giảm giá phải tối thiểu là 1")
    private Integer discount;
}

