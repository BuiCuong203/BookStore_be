package com.vn.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateOrderRequest {
    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Payment method is required (COD, VNPAY, MOMO, BANKING)")
    private String methodPayment;

    private String note;

    // Danh sách ID các CartItem được chọn (null hoặc empty = mua tất cả)
    private List<Long> selectedCartItemIds;

    private String shippingMethod;
}

