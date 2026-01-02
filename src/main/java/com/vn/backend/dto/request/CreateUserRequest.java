package com.vn.backend.dto.request;

import com.vn.backend.util.enums.RoleEnum;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateUserRequest {
    String email;
    String password;
    String fullName;
    String avatarUrl;
    RoleEnum role;
    String phone;
    String address;
    Boolean isActive;
}
