package com.vn.backend.controller;

import com.vn.backend.dto.request.CreateCategoryRequest;
import com.vn.backend.dto.request.UpdateCategoryRequest;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.CategoryResponse;
import com.vn.backend.dto.response.PagedResponse;
import com.vn.backend.service.CategoryService;
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
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Category", description = "Category Management APIs")
public class CategoryController {

    CategoryService categoryService;

    /**
     * Get all categories
     */
    @GetMapping
    @Operation(summary = "Get all categories", description = "Get all categories")
    public ApiResponse<PagedResponse<CategoryResponse>> getAllCategories(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        return ApiResponse.<PagedResponse<CategoryResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .data(categoryService.getAllCategories(keyword, pageable))
                .message("Lấy danh sách categories thành công")
                .build();
    }

    /**
     * Get root categories
     */
    @GetMapping("/root")
    @Operation(summary = "Get root categories", description = "Get categories without parent")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getRootCategories() {
        log.info("Getting root categories");

        List<CategoryResponse> categories = categoryService.getRootCategories();

        ApiResponse<List<CategoryResponse>> response = ApiResponse.<List<CategoryResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Các danh mục gốc đã được truy xuất thành công")
                .data(categories)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get child categories by parent ID
     */
    @GetMapping("/children/{parentId}")
    @Operation(summary = "Get child categories", description = "Get categories by parent ID")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getChildCategories(
            @PathVariable Long parentId) {
        log.info("Getting child categories for parent id: {}", parentId);

        List<CategoryResponse> categories = categoryService.getChildCategories(parentId);

        ApiResponse<List<CategoryResponse>> response = ApiResponse.<List<CategoryResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Các danh mục con đã được truy xuất thành công.")
                .data(categories)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get category by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Get a single category by its ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        log.info("Getting category with id: {}", id);

        CategoryResponse category = categoryService.getCategoryById(id);

        ApiResponse<CategoryResponse> response = ApiResponse.<CategoryResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Danh mục được truy xuất thành công")
                .data(category)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Create new category
     */
    @PostMapping
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Create category", description = "Create a new category (Admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        log.info("Creating new category: {}", request.getName());

        CategoryResponse category = categoryService.createCategory(request);

        ApiResponse<CategoryResponse> response = ApiResponse.<CategoryResponse>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Danh mục được tạo thành công")
                .data(category)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update category
     */
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Update category", description = "Update an existing category (Admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        log.info("Updating category with id: {}", id);

        CategoryResponse category = categoryService.updateCategory(id, request);

        ApiResponse<CategoryResponse> response = ApiResponse.<CategoryResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Danh mục được cập nhật thành công")
                .data(category)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Delete category
     */
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Delete category", description = "Delete a category (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        log.info("Deleting category with id: {}", id);

        categoryService.deleteCategory(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Đã xóa danh mục thành công")
                .build();

        return ResponseEntity.ok(response);
    }

    // Search categories với keyword, page, size, sort
    @GetMapping("/search")
    public ApiResponse<List<CategoryResponse>> searchCategories(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ApiResponse.<List<CategoryResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Tìm kiếm categories thành công")
                .data(categoryService.searchCategories(keyword, pageable))
                .build();
    }
}

