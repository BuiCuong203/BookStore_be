package com.vn.backend.repository;

import com.vn.backend.model.InvalidTokens;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InvalidTokenRepository extends JpaRepository<InvalidTokens, String> {
    @Modifying
    @Transactional
    @Query("DELETE FROM InvalidTokens t WHERE t.expiredAt < :time")
    int deleteExpired(@Param("time") LocalDateTime time);
}
