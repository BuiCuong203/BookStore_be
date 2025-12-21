package com.vn.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddToWishListRequest {
    @NotNull(message = "ID sản phẩm là bắt buộc")
    private Long productId;
}

