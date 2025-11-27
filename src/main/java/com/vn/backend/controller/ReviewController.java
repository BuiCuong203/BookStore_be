package com.vn.backend.controller;

import com.vn.backend.dto.request.CreateReviewRequest;
import com.vn.backend.dto.request.UpdateReviewRequest;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.ReviewResponse;
import com.vn.backend.service.ReviewService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Review", description = "Review Management APIs")
public class ReviewController {

    ReviewService reviewService;

    /**
     * Get all reviews (Admin)
     */
    @GetMapping("/admin/all")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Get all reviews", description = "Get all reviews with pagination (Admin only)")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("Admin getting all reviews - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ReviewResponse> reviews = reviewService.getAllReviews(pageable);

        ApiResponse<Page<ReviewResponse>> response = ApiResponse.<Page<ReviewResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Reviews retrieved successfully")
                .data(reviews)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get reviews by product ID
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "Get product reviews", description = "Get all reviews for a specific product")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviewsByProductId(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("Getting reviews for product: {}", productId);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ReviewResponse> reviews = reviewService.getReviewsByProductId(productId, pageable);

        ApiResponse<Page<ReviewResponse>> response = ApiResponse.<Page<ReviewResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Product reviews retrieved successfully")
                .data(reviews)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get reviews by rating
     */
    @GetMapping("/rating/{rating}")
    @Operation(summary = "Get reviews by rating", description = "Get all reviews with specific rating")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviewsByRating(
            @PathVariable int rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("Getting reviews with rating: {}", rating);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ReviewResponse> reviews = reviewService.getReviewsByRating(rating, pageable);

        ApiResponse<Page<ReviewResponse>> response = ApiResponse.<Page<ReviewResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Reviews retrieved successfully")
                .data(reviews)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's reviews
     */
    @GetMapping("/my-reviews")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Get my reviews", description = "Get current user's reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getUserReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("Getting user reviews");

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ReviewResponse> reviews = reviewService.getUserReviews(pageable);

        ApiResponse<Page<ReviewResponse>> response = ApiResponse.<Page<ReviewResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Your reviews retrieved successfully")
                .data(reviews)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get review by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get review by ID", description = "Get a single review by its ID")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewById(@PathVariable Long id) {
        log.info("Getting review with id: {}", id);

        ReviewResponse review = reviewService.getReviewById(id);

        ApiResponse<ReviewResponse> response = ApiResponse.<ReviewResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Review retrieved successfully")
                .data(review)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get product rating statistics
     */
    @GetMapping("/product/{productId}/stats")
    @Operation(summary = "Get product rating stats", description = "Get rating statistics for a product")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductRatingStats(
            @PathVariable Long productId) {
        log.info("Getting rating stats for product: {}", productId);

        Map<String, Object> stats = reviewService.getProductRatingStats(productId);

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Rating statistics retrieved successfully")
                .data(stats)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Create review
     */
    @PostMapping
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Create review", description = "Create a new review for a purchased product")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        log.info("Creating new review for order item: {}", request.getOrderItemId());

        ReviewResponse review = reviewService.createReview(request);

        ApiResponse<ReviewResponse> response = ApiResponse.<ReviewResponse>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Review created successfully")
                .data(review)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update review
     */
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Update review", description = "Update your own review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request) {
        log.info("Updating review with id: {}", id);

        ReviewResponse review = reviewService.updateReview(id, request);

        ApiResponse<ReviewResponse> response = ApiResponse.<ReviewResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Review updated successfully")
                .data(review)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Delete review
     */
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Delete review", description = "Delete your own review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        log.info("Deleting review with id: {}", id);

        reviewService.deleteReview(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Review deleted successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Delete review by admin
     */
    @DeleteMapping("/admin/{id}")
    @SecurityRequirement(name = "bearer-key")
    @Operation(summary = "Delete review (Admin)", description = "Delete any review (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteReviewByAdmin(@PathVariable Long id) {
        log.info("Admin deleting review with id: {}", id);

        reviewService.deleteReviewByAdmin(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Review deleted successfully")
                .build();

        return ResponseEntity.ok(response);
    }
}

