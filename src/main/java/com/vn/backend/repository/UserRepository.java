package com.vn.backend.repository;

import com.vn.backend.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("""
              SELECT DISTINCT u FROM User u
              LEFT JOIN FETCH u.roles r
              LEFT JOIN FETCH r.permissions
              WHERE u.email = :email
            """)
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByGoogleId(String googleId);

    @Query("""
                select u from User u
                left join fetch u.roles
                where u.email = :email
            """)
    Optional<User> findByEmailWithRoles(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate AND u.createdAt <= :endDate")
    Long countUsersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
