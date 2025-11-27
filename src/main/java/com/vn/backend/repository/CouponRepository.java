package com.vn.backend.repository;

import com.vn.backend.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
    boolean existsByCode(String code);
    List<Coupon> findByStockQuantityGreaterThan(int quantity);
    List<Coupon> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

