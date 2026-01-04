package com.vn.backend.controller;

import com.vn.backend.dto.request.CreateProductRequest;
import com.vn.backend.dto.request.UpdateProductRequest;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.PagedResponse;
import com.vn.backend.dto.response.ProductResponse;
import com.vn.backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Product", description = "Product Management APIs")
public class ProductController {

    ProductService productService;

    /**
     * Get all products with pagination
     */
    @GetMapping
    @Operation(summary = "Get all products", description = "Get all products with pagination and sorting")
    public ApiResponse<PagedResponse<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        return ApiResponse.<PagedResponse<ProductResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .data(productService.getAllProducts(keyword, pageable))
                .message("Lấy danh sách products thành công")
                .build();
    }

    /**
     * Get product by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Get a single product by its ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        log.info("Getting product with id: {}", id);

        ProductResponse product = productService.getProductById(id);

        ApiResponse<ProductResponse> response = ApiResponse.<ProductResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Sản phẩm được lấy thành công")
                .data(product)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Create new product
     */
    @PostMapping
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Create product", description = "Create a new product (Admin only)")
    public ApiResponse<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse product = productService.createProduct(request);

        return ApiResponse.<ProductResponse>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Sản phẩm được tạo thành công")
                .data(product)
                .build();
    }


    /**
     * Update product
     */
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Update product", description = "Update an existing product (Admin only)")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("Updating product with id: {}", id);

        ProductResponse product = productService.updateProduct(id, request);

        return ApiResponse.<ProductResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Sản phẩm được cập nhật thành công")
                .data(product)
                .build();
    }

    /**
     * Delete product
     */
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Delete product", description = "Delete a product (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        log.info("Deleting product with id: {}", id);

        productService.deleteProduct(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Đã xóa sản phẩm thành công")
                .build();

        return ResponseEntity.ok(response);
    }
}

