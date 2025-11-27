package com.vn.backend.service;

import com.vn.backend.dto.request.CreateReviewRequest;
import com.vn.backend.dto.request.UpdateReviewRequest;
import com.vn.backend.dto.response.ReviewResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.*;
import com.vn.backend.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReviewService {

    ReviewRepository reviewRepository;
    OrderItemRepository orderItemRepository;
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
     * Convert Review to ReviewResponse
     */
    private ReviewResponse toReviewResponse(Review review) {
        User user = review.getOrderItem().getOrder().getUser();
        Product product = review.getProduct();

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(product.getId())
                .productName(product.getName())
                .userId(user.getId())
                .userName(user.getFullName())
                .userEmail(user.getEmail())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * Update product rating statistics
     */
    private void updateProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Product not found"));

        Double avgRating = reviewRepository.getAverageRatingByProductId(productId);
        Long reviewCount = reviewRepository.countByProductId(productId);

        product.setRatingAvg(avgRating != null ? avgRating : 0.0);
        product.setRatingCount(reviewCount != null ? reviewCount.intValue() : 0);
        productRepository.save(product);

        log.info("Updated product {} rating - Avg: {}, Count: {}",
            productId, product.getRatingAvg(), product.getRatingCount());
    }

    /**
     * Get all reviews with pagination (Admin)
     */
    public Page<ReviewResponse> getAllReviews(Pageable pageable) {
        log.info("Getting all reviews with pagination");
        return reviewRepository.findAll(pageable)
                .map(this::toReviewResponse);
    }

    /**
     * Get reviews by product ID
     */
    public Page<ReviewResponse> getReviewsByProductId(Long productId, Pageable pageable) {
        log.info("Getting reviews for product: {}", productId);

        if (!productRepository.existsById(productId)) {
            throw new AppException(HttpStatus.NOT_FOUND.value(), "Product not found");
        }

        return reviewRepository.findByProductId(productId, pageable)
                .map(this::toReviewResponse);
    }

    /**
     * Get reviews by rating
     */
    public Page<ReviewResponse> getReviewsByRating(int rating, Pageable pageable) {
        log.info("Getting reviews with rating: {}", rating);

        if (rating < 1 || rating > 5) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Rating must be between 1 and 5");
        }

        return reviewRepository.findByRating(rating, pageable)
                .map(this::toReviewResponse);
    }

    /**
     * Get user's reviews
     */
    public Page<ReviewResponse> getUserReviews(Pageable pageable) {
        User currentUser = getCurrentUser();
        log.info("Getting reviews for user: {}", currentUser.getEmail());

        return reviewRepository.findByUser(currentUser, pageable)
                .map(this::toReviewResponse);
    }

    /**
     * Get review by ID
     */
    public ReviewResponse getReviewById(Long id) {
        log.info("Getting review with id: {}", id);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Review not found"));

        return toReviewResponse(review);
    }

    /**
     * Get product rating statistics
     */
    public Map<String, Object> getProductRatingStats(Long productId) {
        log.info("Getting rating statistics for product: {}", productId);

        if (!productRepository.existsById(productId)) {
            throw new AppException(HttpStatus.NOT_FOUND.value(), "Product not found");
        }

        Double avgRating = reviewRepository.getAverageRatingByProductId(productId);
        Long totalReviews = reviewRepository.countByProductId(productId);

        // Count reviews by star rating
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            List<Review> reviews = reviewRepository.findByRating(i);
            long count = reviews.stream()
                    .filter(r -> r.getProduct().getId().equals(productId))
                    .count();
            ratingDistribution.put(i, count);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", avgRating != null ? avgRating : 0.0);
        stats.put("totalReviews", totalReviews != null ? totalReviews : 0);
        stats.put("ratingDistribution", ratingDistribution);

        return stats;
    }

    /**
     * Create review
     */
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        User currentUser = getCurrentUser();
        log.info("Creating review for order item: {}", request.getOrderItemId());

        // Get order item
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Order item not found"));

        // Verify order belongs to current user
        if (!orderItem.getOrder().getUser().getId().equals(currentUser.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                "You can only review products you have purchased");
        }

        // Verify order is delivered
        if (!"DELIVERED".equals(orderItem.getOrder().getStatus())) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                "You can only review delivered orders");
        }

        // Check if review already exists
        Product product = orderItem.getProduct();
        if (reviewRepository.findByProductIdAndUser(product.getId(), currentUser).isPresent()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(),
                "You have already reviewed this product");
        }

        // Create review
        Review review = Review.builder()
                .orderItem(orderItem)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);
        log.info("Review created successfully with id: {}", review.getId());

        // Update product rating
        updateProductRating(product.getId());

        return toReviewResponse(review);
    }

    /**
     * Update review
     */
    @Transactional
    public ReviewResponse updateReview(Long id, UpdateReviewRequest request) {
        User currentUser = getCurrentUser();
        log.info("Updating review with id: {}", id);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Review not found"));

        // Verify review belongs to current user
        if (!review.getOrderItem().getOrder().getUser().getId().equals(currentUser.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                "You can only update your own reviews");
        }

        // Update fields if provided
        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        review = reviewRepository.save(review);
        log.info("Review updated successfully with id: {}", review.getId());

        // Update product rating
        updateProductRating(review.getProduct().getId());

        return toReviewResponse(review);
    }

    /**
     * Delete review
     */
    @Transactional
    public void deleteReview(Long id) {
        User currentUser = getCurrentUser();
        log.info("Deleting review with id: {}", id);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Review not found"));

        // Verify review belongs to current user
        if (!review.getOrderItem().getOrder().getUser().getId().equals(currentUser.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                "You can only delete your own reviews");
        }

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        log.info("Review deleted successfully with id: {}", id);

        // Update product rating
        updateProductRating(productId);
    }

    /**
     * Delete review by admin
     */
    @Transactional
    public void deleteReviewByAdmin(Long id) {
        log.info("Admin deleting review with id: {}", id);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Review not found"));

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        log.info("Review deleted successfully by admin with id: {}", id);

        // Update product rating
        updateProductRating(productId);
    }
}

