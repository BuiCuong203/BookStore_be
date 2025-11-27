package com.vn.backend.service;

import com.vn.backend.dto.request.AddToWishListRequest;
import com.vn.backend.dto.response.WishListItemResponse;
import com.vn.backend.dto.response.WishListResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.Product;
import com.vn.backend.model.User;
import com.vn.backend.model.WishList;
import com.vn.backend.repository.ProductRepository;
import com.vn.backend.repository.UserRepository;
import com.vn.backend.repository.WishListRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WishListService {

    WishListRepository wishListRepository;
    ProductRepository productRepository;
    UserRepository userRepository;

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "User not found"));
    }

    /**
     * Convert WishList to WishListItemResponse
     */
    private WishListItemResponse toWishListItemResponse(WishList wishList) {
        Product product = wishList.getProduct();
        long originalPrice = product.getPrice();
        int discount = product.getDiscount();
        long finalPrice = originalPrice - (originalPrice * discount / 100);

        return WishListItemResponse.builder()
                .id(wishList.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productPrice(product.getPrice())
                .productDiscount(product.getDiscount())
                .finalPrice(finalPrice)
                .stockQuantity(product.getStockQuanity())
                .inStock(product.getStockQuanity() > 0)
                .build();
    }

    /**
     * Get user's wishlist
     */
    public WishListResponse getWishList() {
        User currentUser = getCurrentUser();
        log.info("Getting wishlist for user: {}", currentUser.getEmail());

        List<WishList> wishListItems = wishListRepository.findByUser(currentUser);

        List<WishListItemResponse> itemResponses = wishListItems.stream()
                .map(this::toWishListItemResponse)
                .collect(Collectors.toList());

        return WishListResponse.builder()
                .userId(currentUser.getId())
                .userName(currentUser.getFullName())
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .build();
    }

    /**
     * Check if product is in wishlist
     */
    public boolean isProductInWishList(Long productId) {
        User currentUser = getCurrentUser();
        log.info("Checking if product {} is in wishlist for user: {}", productId, currentUser.getEmail());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Product not found"));

        return wishListRepository.existsByUserAndProduct(currentUser, product);
    }

    /**
     * Add product to wishlist
     */
    @Transactional
    public WishListResponse addToWishList(AddToWishListRequest request) {
        User currentUser = getCurrentUser();
        log.info("Adding product {} to wishlist for user: {}", request.getProductId(), currentUser.getEmail());

        // Check if product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Product not found"));

        // Check if product already in wishlist
        if (wishListRepository.existsByUserAndProduct(currentUser, product)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                "Product is already in your wishlist");
        }

        // Add to wishlist
        WishList wishList = WishList.builder()
                .user(currentUser)
                .product(product)
                .build();

        wishListRepository.save(wishList);
        log.info("Product added to wishlist successfully");

        return getWishList();
    }

    /**
     * Remove product from wishlist
     */
    @Transactional
    public WishListResponse removeFromWishList(Long productId) {
        User currentUser = getCurrentUser();
        log.info("Removing product {} from wishlist for user: {}", productId, currentUser.getEmail());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Product not found"));

        WishList wishList = wishListRepository.findByUserAndProduct(currentUser, product)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(),
                    "Product not found in wishlist"));

        wishListRepository.delete(wishList);
        log.info("Product removed from wishlist successfully");

        return getWishList();
    }

    /**
     * Remove item from wishlist by ID
     */
    @Transactional
    public WishListResponse removeItemById(Long itemId) {
        User currentUser = getCurrentUser();
        log.info("Removing wishlist item {} for user: {}", itemId, currentUser.getEmail());

        WishList wishList = wishListRepository.findById(itemId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(),
                    "Wishlist item not found"));

        // Verify item belongs to current user
        if (!wishList.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                "This item does not belong to your wishlist");
        }

        wishListRepository.delete(wishList);
        log.info("Wishlist item removed successfully");

        return getWishList();
    }

    /**
     * Clear all items from wishlist
     */
    @Transactional
    public void clearWishList() {
        User currentUser = getCurrentUser();
        log.info("Clearing wishlist for user: {}", currentUser.getEmail());

        List<WishList> wishListItems = wishListRepository.findByUser(currentUser);
        wishListRepository.deleteAll(wishListItems);

        log.info("Wishlist cleared successfully");
    }

    /**
     * Get wishlist item count
     */
    public Long getWishListCount() {
        User currentUser = getCurrentUser();
        log.info("Getting wishlist count for user: {}", currentUser.getEmail());

        return wishListRepository.countByUser(currentUser);
    }
}

