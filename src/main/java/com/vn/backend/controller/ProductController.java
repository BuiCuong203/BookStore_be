package com.vn.backend.controller;

import com.vn.backend.dto.ai.ProductFieldRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vn.backend.dto.request.CreateProductRequest;
import com.vn.backend.dto.request.UpdateProductRequest;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.PagedResponse;
import com.vn.backend.dto.response.ProductResponse;
import com.vn.backend.model.Product;
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

import java.util.List;

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

    /**
     * Get products by star rating
     */
    @GetMapping("/by-stars/{stars}")
    @Operation(summary = "Get products by star rating",
               description = "Get products filtered by star rating (1-5 stars)")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProductsByStars(
            @PathVariable Integer stars,
            Pageable pageable) {
        log.info("Getting products with {} stars", stars);

        var products = productService.getProductsByStarRating(stars, pageable);

        PagedResponse<ProductResponse> pagedResponse = PagedResponse.<ProductResponse>builder()
                .data(products.getContent())
                .totalElements(products.getTotalElements())
                .totalPages(products.getTotalPages())
                .currentPage(products.getNumber())
                .pageSize(products.getSize())
                .hasNext(products.hasNext())
                .hasPrevious(products.hasPrevious())
                .build();

        ApiResponse<PagedResponse<ProductResponse>> response = ApiResponse.<PagedResponse<ProductResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Lấy danh sách sản phẩm " + stars + " sao thành công")
                .data(pagedResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get products by price range
     */
    @GetMapping("/by-price-range")
    @Operation(summary = "Get products by price range",
               description = "Get products filtered by price range (minPrice to maxPrice)")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProductsByPriceRange(
            @RequestParam(required = false, defaultValue = "0") Long minPrice,
            @RequestParam(required = false, defaultValue = "1000000") Long maxPrice,
            Pageable pageable) {
        log.info("Getting products with price range: {} - {}", minPrice, maxPrice);

        var products = productService.getProductsByPriceRange(minPrice, maxPrice, pageable);

        PagedResponse<ProductResponse> pagedResponse = PagedResponse.<ProductResponse>builder()
                .data(products.getContent())
                .totalElements(products.getTotalElements())
                .totalPages(products.getTotalPages())
                .currentPage(products.getNumber())
                .pageSize(products.getSize())
                .hasNext(products.hasNext())
                .hasPrevious(products.hasPrevious())
                .build();

        ApiResponse<PagedResponse<ProductResponse>> response = ApiResponse.<PagedResponse<ProductResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Lấy danh sách sản phẩm theo khoảng giá thành công")
                .data(pagedResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get products by category ID
     */
    @GetMapping("/by-category/{categoryId}")
    @Operation(summary = "Get products by category",
               description = "Get all products filtered by category ID")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProductsByCategory(
            @PathVariable Long categoryId,
            Pageable pageable) {
        log.info("Getting products by category id: {}", categoryId);

        var products = productService.getProductsByCategory(categoryId, pageable);

        PagedResponse<ProductResponse> pagedResponse = PagedResponse.<ProductResponse>builder()
                .data(products.getContent())
                .totalElements(products.getTotalElements())
                .totalPages(products.getTotalPages())
                .currentPage(products.getNumber())
                .pageSize(products.getSize())
                .hasNext(products.hasNext())
                .hasPrevious(products.hasPrevious())
                .build();

        ApiResponse<PagedResponse<ProductResponse>> response = ApiResponse.<PagedResponse<ProductResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Lấy danh sách sản phẩm theo danh mục thành công")
                .data(pagedResponse)
                .build();

        return ResponseEntity.ok(response);
    }
    /**
     * Semantic search products
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> search(@RequestParam String q) {

        List<ProductResponse> result = productService.searchBySemanticSimilarity(q);

        ApiResponse<List<ProductResponse>> response = ApiResponse.<List<ProductResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Tìm kiếm thành công")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get similar products by product ID
     */
    @GetMapping("/product/{id}/similar")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getSimilar(@PathVariable Long id) {

        List<ProductResponse> result = productService.getSimilarBooks(id);

        ApiResponse<List<ProductResponse>> response = ApiResponse.<List<ProductResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Lấy danh sách gợi ý thành công")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }
}

