package com.vn.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String shortDescription;
    private String description;
    private String dimension;
    private Integer numberOfPages;
    private String isbn;
    private Integer stockQuantity;
    private Long price;
    private Integer discount;
    private Long finalPrice;
    private String publisher;
    private LocalDateTime publisherDate;
    private Double ratingAvg;
    private Integer ratingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<AuthorResponse> authors;
    private List<ProductImageResponse> images;
}

