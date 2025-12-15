package com.vn.backend.controller;

import com.vn.backend.dto.request.AddToWishListRequest;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.WishListResponse;
import com.vn.backend.service.WishListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "WishList", description = "WishList Management APIs")
@SecurityRequirement(name = "bearer-key")
public class WishListController {

    WishListService wishListService;

    /**
     * Get user's wishlist
     */
    @GetMapping
    @Operation(summary = "Get wishlist", description = "Get current user's wishlist")
    public ResponseEntity<ApiResponse<WishListResponse>> getWishList() {
        log.info("Getting wishlist for current user");

        WishListResponse wishList = wishListService.getWishList();

        ApiResponse<WishListResponse> response = ApiResponse.<WishListResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Danh sách yêu thích đã được truy xuất thành công.")
                .data(wishList)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get wishlist count
     */
    @GetMapping("/count")
    @Operation(summary = "Get wishlist count", description = "Get total items in wishlist")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getWishListCount() {
        log.info("Getting wishlist count");

        Long count = wishListService.getWishListCount();

        ApiResponse<Map<String, Long>> response = ApiResponse.<Map<String, Long>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Số lượng sản phẩm trong danh sách yêu thích đã được truy xuất thành công.")
                .data(Map.of("count", count))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Check if product is in wishlist
     */
    @GetMapping("/check/{productId}")
    @Operation(summary = "Check product in wishlist", description = "Check if a product is in user's wishlist")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkProductInWishList(
            @PathVariable Long productId) {
        log.info("Checking if product {} is in wishlist", productId);

        boolean inWishList = wishListService.isProductInWishList(productId);

        ApiResponse<Map<String, Boolean>> response = ApiResponse.<Map<String, Boolean>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Kiểm tra hoàn tất thành công")
                .data(Map.of("inWishList", inWishList))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Add product to wishlist
     */
    @PostMapping
    @Operation(summary = "Add to wishlist", description = "Add a product to wishlist")
    public ResponseEntity<ApiResponse<WishListResponse>> addToWishList(
            @Valid @RequestBody AddToWishListRequest request) {
        log.info("Adding product {} to wishlist", request.getProductId());

        WishListResponse wishList = wishListService.addToWishList(request);

        ApiResponse<WishListResponse> response = ApiResponse.<WishListResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Sản phẩm đã được thêm vào danh sách yêu thích thành công.")
                .data(wishList)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Remove product from wishlist by product ID
     */
    @DeleteMapping("/product/{productId}")
    @Operation(summary = "Remove by product ID", description = "Remove a product from wishlist by product ID")
    public ResponseEntity<ApiResponse<WishListResponse>> removeFromWishList(
            @PathVariable Long productId) {
        log.info("Removing product {} from wishlist", productId);

        WishListResponse wishList = wishListService.removeFromWishList(productId);

        ApiResponse<WishListResponse> response = ApiResponse.<WishListResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Sản phẩm đã được xóa khỏi danh sách yêu thích thành công.")
                .data(wishList)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Remove item from wishlist by item ID
     */
    @DeleteMapping("/item/{itemId}")
    @Operation(summary = "Remove by item ID", description = "Remove an item from wishlist by item ID")
    public ResponseEntity<ApiResponse<WishListResponse>> removeItemById(
            @PathVariable Long itemId) {
        log.info("Removing wishlist item {}", itemId);

        WishListResponse wishList = wishListService.removeItemById(itemId);

        ApiResponse<WishListResponse> response = ApiResponse.<WishListResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Sản phẩm đã được xóa khỏi danh sách yêu thích thành công.")
                .data(wishList)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Clear all items from wishlist
     */
    @DeleteMapping
    @Operation(summary = "Clear wishlist", description = "Remove all items from wishlist")
    public ResponseEntity<ApiResponse<Void>> clearWishList() {
        log.info("Clearing wishlist");

        wishListService.clearWishList();

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Đã xóa danh sách yêu thích thành công")
                .build();

        return ResponseEntity.ok(response);
    }
}

