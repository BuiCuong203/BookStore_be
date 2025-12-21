package com.vn.backend.controller;

import com.vn.backend.dto.request.ApplyCouponRequest;
import com.vn.backend.dto.request.CreateCouponRequest;
import com.vn.backend.dto.request.UpdateCouponRequest;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.CouponResponse;
import com.vn.backend.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Coupon", description = "Coupon Management APIs")
public class CouponController {

    CouponService couponService;

    /**
     * Get all coupons with pagination (Admin)
     */
    @GetMapping("/admin/all")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Get all coupons", description = "Get all coupons with pagination (Admin only)")
    public ResponseEntity<ApiResponse<Page<CouponResponse>>> getAllCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("Admin getting all coupons - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<CouponResponse> coupons = couponService.getAllCoupons(pageable);

        ApiResponse<Page<CouponResponse>> response = ApiResponse.<Page<CouponResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Mã giảm giá đã được truy xuất thành công")
                .data(coupons)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get available coupons (Public)
     */
    @GetMapping("/available")
    @Operation(summary = "Get available coupons", description = "Get all coupons with stock > 0")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAvailableCoupons() {
        log.info("Getting available coupons");

        List<CouponResponse> coupons = couponService.getAvailableCoupons();

        ApiResponse<List<CouponResponse>> response = ApiResponse.<List<CouponResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Đã truy xuất thành công các phiếu giảm giá hiện có.")
                .data(coupons)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get coupon by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get coupon by ID", description = "Get a single coupon by its ID")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponById(@PathVariable Long id) {
        log.info("Getting coupon with id: {}", id);

        CouponResponse coupon = couponService.getCouponById(id);

        ApiResponse<CouponResponse> response = ApiResponse.<CouponResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Mã giảm giá đã được lấy thành công")
                .data(coupon)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get coupon by code
     */
    @GetMapping("/code/{code}")
    @Operation(summary = "Get coupon by code", description = "Get a coupon by its code")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponByCode(@PathVariable String code) {
        log.info("Getting coupon with code: {}", code);

        CouponResponse coupon = couponService.getCouponByCode(code);

        ApiResponse<CouponResponse> response = ApiResponse.<CouponResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Mã giảm giá đã được lấy thành công")
                .data(coupon)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Validate coupon
     */
    @PostMapping("/validate")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Validate coupon", description = "Check if coupon is valid and available")
    public ResponseEntity<ApiResponse<CouponResponse>> validateCoupon(
            @Valid @RequestBody ApplyCouponRequest request) {
        log.info("Validating coupon: {}", request.getCouponCode());

        CouponResponse coupon = couponService.validateCoupon(request.getCouponCode());

        ApiResponse<CouponResponse> response = ApiResponse.<CouponResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Mã giảm giá vẫn còn hiệu lực và có thể sử dụng.")
                .data(coupon)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Calculate discount
     */
    @PostMapping("/calculate")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Calculate discount", description = "Calculate discount amount for a coupon")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateDiscount(
            @RequestParam String code,
            @RequestParam Long amount) {
        log.info("Calculating discount for coupon: {} with amount: {}", code, amount);

        Long discountAmount = couponService.calculateDiscount(code, amount);
        Long finalAmount = amount - discountAmount;

        Map<String, Object> result = Map.of(
                "originalAmount", amount,
                "discountAmount", discountAmount,
                "finalAmount", finalAmount,
                "couponCode", code
        );

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Tính chiết khấu thành công")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Create coupon (Admin)
     */
    @PostMapping
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Create coupon", description = "Create a new coupon (Admin only)")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CreateCouponRequest request) {
        log.info("Creating new coupon: {}", request.getCode());

        CouponResponse coupon = couponService.createCoupon(request);

        ApiResponse<CouponResponse> response = ApiResponse.<CouponResponse>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Phiếu giảm giá được tạo thành công")
                .data(coupon)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update coupon (Admin)
     */
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Update coupon", description = "Update an existing coupon (Admin only)")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCouponRequest request) {
        log.info("Updating coupon with id: {}", id);

        CouponResponse coupon = couponService.updateCoupon(id, request);

        ApiResponse<CouponResponse> response = ApiResponse.<CouponResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Phiếu giảm giá được cập nhật thành công")
                .data(coupon)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Delete coupon (Admin)
     */
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Delete coupon", description = "Delete a coupon (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        log.info("Deleting coupon with id: {}", id);

        couponService.deleteCoupon(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Đã xóa phiếu giảm giá thành công")
                .build();

        return ResponseEntity.ok(response);
    }
}

