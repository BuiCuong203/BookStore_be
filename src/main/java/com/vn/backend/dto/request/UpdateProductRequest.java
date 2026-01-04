package com.vn.backend.dto.request;

import jakarta.validation.constraints.Min;
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
public class UpdateProductRequest {

    private Long categoryId;

    private String name;

    private String shortDescription;

    private String description;

    private String dimension;

    @Min(value = 0, message = "Number of pages must be at least 0")
    private Integer numberOfPages;

    private String isbn;

    @Min(value = 0, message = "Stock quantity must be at least 0")
    private Integer stockQuantity;

    @Min(value = 0, message = "Price must be at least 0")
    private Long price;

    @Min(value = 0, message = "Discount must be at least 0")
    private Integer discount;

    private String publisher;

    private LocalDateTime publisherDate;

    private List<CreateAuthorRequest> authors;

    private List<CreateProductImageRequest> images;
}

