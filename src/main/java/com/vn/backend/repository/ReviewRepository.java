package com.vn.backend.repository;

import com.vn.backend.model.Product;
import com.vn.backend.model.Review;
import com.vn.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct(Product product);
    Page<Review> findByProduct(Product product, Pageable pageable);
    List<Review> findByProductId(Long productId);
    Page<Review> findByProductId(Long productId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.orderItem.order.user = :user")
    List<Review> findByUser(User user);

    @Query("SELECT r FROM Review r WHERE r.orderItem.order.user = :user")
    Page<Review> findByUser(User user, Pageable pageable);

    List<Review> findByRating(int rating);
    Page<Review> findByRating(int rating, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.orderItem.order.user = :user")
    Optional<Review> findByProductIdAndUser(Long productId, User user);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRatingByProductId(Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Long countByProductId(Long productId);
}

