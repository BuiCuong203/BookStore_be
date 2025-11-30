package com.vn.backend.repository;

import com.vn.backend.model.Product;
import com.vn.backend.model.User;
import com.vn.backend.model.WishList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishListRepository extends JpaRepository<WishList, Long> {
    List<WishList> findByUser(User user);
    List<WishList> findByUserId(Long userId);
    Optional<WishList> findByUserAndProduct(User user, Product product);
    Optional<WishList> findByUserIdAndProductId(Long userId, Long productId);
    boolean existsByUserAndProduct(User user, Product product);
    void deleteByUserIdAndProductId(Long userId, Long productId);
    long countByUser(User user);
}

