package com.vn.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateProductRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Product name is required")
    private String name;

    private String shortDescription;

    private String description;

    @NotBlank(message = "Dimension is required")
    private String dimension;

    @Min(value = 0, message = "Number of pages must be at least 0")
    private Integer numberOfPages;

    @NotBlank(message = "ISBN is required")
    private String isbn;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be at least 0")
    private Integer stockQuantity;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be at least 0")
    private Long price;

    @Min(value = 0, message = "Discount must be at least 0")
    private Integer discount = 0;

    @NotBlank(message = "Publisher is required")
    private String publisher;

    @NotNull(message = "Publisher date is required")
    private LocalDateTime publisherDate;

    private List<CreateAuthorRequest> authors;
    
    private List<CreateProductImageRequest> images;
}

