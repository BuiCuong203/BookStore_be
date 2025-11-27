package com.vn.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WishListItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private Long productPrice;
    private Integer productDiscount;
    private Long finalPrice;
    private Integer stockQuantity;
    private Boolean inStock;
}

