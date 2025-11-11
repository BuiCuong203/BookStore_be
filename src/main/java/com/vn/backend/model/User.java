package com.vn.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", nullable = false, length = 255)
    String password;

    @Column(name = "full_name", nullable = false, length = 255)
    String fullName;

    @Column(name = "avatar_url")
    String avatarUrl;

    @Column(name = "phone", length = 20)
    String phone;

    @Column(name = "address", length = 255)
    String address;

    @Column(name = "is_active", nullable = false)
    boolean isActive;

    @Column(name = "is_locked", nullable = false)
    boolean isLocked;

    @Column(name = "mfa_enabled", nullable = false)
    boolean mfaEnabled;

    @Column(name = "otp", length = 6)
    String otp;

    @Column(name = "otp_expires_at")
    LocalDateTime otpExpiresAt;

    @Column(name = "otp_consumed", nullable = false)
    boolean otpConsumed;

    @Column(name = "last_login_at")
    LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_roles_user")),
            inverseJoinColumns = @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_user_roles_role"))
    )
    private Set<Role> roles = new HashSet<>();

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Cart cart;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
