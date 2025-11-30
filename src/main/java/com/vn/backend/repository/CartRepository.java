package com.vn.backend.repository;

import com.vn.backend.model.Cart;
import com.vn.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomer(User customer);
    Optional<Cart> findByCustomerId(Long customerId);
    boolean existsByCustomerId(Long customerId);
}

