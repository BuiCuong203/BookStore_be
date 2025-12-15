package com.vn.backend.controller;

import com.vn.backend.dto.request.AddToCartRequest;
import com.vn.backend.dto.request.UpdateCartItemRequest;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.CartResponse;
import com.vn.backend.service.CartService;
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

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Cart", description = "Cart Management APIs")
@SecurityRequirement(name = "bearer-key")
public class CartController {

    CartService cartService;

    /**
     * Get user's cart
     */
    @GetMapping
    @Operation(summary = "Get cart", description = "Get current user's shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        log.info("Getting cart for current user");
        CartResponse cart = cartService.getCart();

        ApiResponse<CartResponse> response = ApiResponse.<CartResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Cart retrieved successfully")
                .data(cart)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Add item to cart
     */
    @PostMapping("/items")
    @Operation(summary = "Add to cart", description = "Add a product to shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request) {
        log.info("Adding product {} to cart with quantity {}",
                request.getProductId(), request.getQuantity());

        CartResponse cart = cartService.addToCart(request);

        ApiResponse<CartResponse> response = ApiResponse.<CartResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Sách được thêm va giỏ hàng thành công")
                .data(cart)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Update cart item quantity
     */
    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item", description = "Update quantity of an item in cart")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        log.info("Updating cart item {} with quantity {}", itemId, request.getQuantity());

        CartResponse cart = cartService.updateCartItem(itemId, request);

        ApiResponse<CartResponse> response = ApiResponse.<CartResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Cập nhật giỏ hàng thành công")
                .data(cart)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Remove item from cart
     */
    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove from cart", description = "Remove an item from shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(@PathVariable Long itemId) {
        log.info("Removing cart item {}", itemId);

        CartResponse cart = cartService.removeCartItem(itemId);

        ApiResponse<CartResponse> response = ApiResponse.<CartResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Xóa item thành công")
                .data(cart)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Clear all items from cart
     */
    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Remove all items from shopping cart")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        log.info("Clearing cart for current user");

        cartService.clearCart();

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Xóa giỏ hàng thành công")
                .build();

        return ResponseEntity.ok(response);
    }
}
