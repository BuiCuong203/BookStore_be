package com.vn.backend.dto.response;

import com.vn.backend.util.enums.RoleEnum;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    Long id;
    String email;
    String fullName;
    String avatarUrl;
    String phone;
    String address;
    boolean isActive;
    RoleEnum role;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
