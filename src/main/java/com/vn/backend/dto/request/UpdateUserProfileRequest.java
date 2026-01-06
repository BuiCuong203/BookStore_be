package com.vn.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserProfileRequest {
    private String fullName;
    private String phone;
    private String address;
    private String avatarUrl;
}
