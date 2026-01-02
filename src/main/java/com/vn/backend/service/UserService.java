package com.vn.backend.service;

import com.vn.backend.dto.request.CreateUserRequest;
import com.vn.backend.dto.request.UpdateUserRequest;
import com.vn.backend.dto.response.PagedResponse;
import com.vn.backend.dto.response.UserResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.Role;
import com.vn.backend.model.User;
import com.vn.backend.repository.RoleRepository;
import com.vn.backend.repository.UserRepository;
import com.vn.backend.util.enums.RoleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    // 1. Lấy danh sách users với phân trang và tìm kiếm
    public PagedResponse<UserResponse> getAllUsers(String keyword, Pageable pageable) {
        return getAllUsers(keyword, pageable, false); // Mặc định hiển thị tất cả users
    }

    // 1a. Lấy danh sách users với tùy chọn chỉ hiển thị user active
    public PagedResponse<UserResponse> getAllUsers(String keyword, Pageable pageable, boolean activeOnly) {

        // Tạo sort mặc định nếu không có sort parameter
        Pageable pageableWithDefaultSort = pageable;
        if (pageable.getSort().isUnsorted()) {
            Sort defaultSort = Sort.by(Sort.Direction.DESC, "createdAt");
            pageableWithDefaultSort = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    defaultSort
            );
        }

        Page<User> users;

        if (activeOnly) {
            // Chỉ lấy user active
            if (keyword != null && !keyword.trim().isEmpty()) {
                users = userRepository.findActiveUsersByKeyword(keyword.trim(), pageableWithDefaultSort);
            } else {
                users = userRepository.findAllActiveUsers(pageableWithDefaultSort);
            }
        } else {
            // Lấy tất cả users (bao gồm cả inactive)
            if (keyword != null && !keyword.trim().isEmpty()) {
                users = userRepository.findByKeyword(keyword.trim(), pageableWithDefaultSort);
            } else {
                users = userRepository.findAll(pageableWithDefaultSort);
            }
        }

        List<UserResponse> userResponses = users.getContent().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());

        PagedResponse<UserResponse> response = PagedResponse.<UserResponse>builder()
                .data(userResponses)
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .currentPage(users.getNumber())
                .pageSize(users.getSize())
                .hasNext(users.hasNext())
                .hasPrevious(users.hasPrevious())
                .build();

        return response;
    }

    // 2. Lấy user theo ID
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Không tìm thấy user với ID: " + id));
        UserResponse response = convertToUserResponse(user);
        return response;
    }

    // 3. Tạo user mới
    public UserResponse createUser(CreateUserRequest request) {

        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.CONFLICT.value(), "Email đã tồn tại: " + request.getEmail());
        }

        RoleEnum roleEnum = request.getRole() != null
                ? request.getRole()
                : RoleEnum.ROLE_USER; // default

        Role role = roleRepository.findByName(roleEnum.name().replace("ROLE_", ""))
                .orElseThrow(() ->
                        new AppException(
                                HttpStatus.BAD_REQUEST.value(),
                                "Role không hợp lệ: " + roleEnum
                        )
                );

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = User.builder()
                .email(request.getEmail())
                .password(encoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .roles(roles)
                .address(request.getAddress())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .avatarUrl(request.getAvatarUrl())
                .build();

        User savedUser = userRepository.save(user);
        UserResponse response = convertToUserResponse(savedUser);

        return response;
    }

    // 4. Cập nhật user
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Không tìm thấy user với ID: " + id));

        // Cập nhật các field nếu có
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getIsActive() != null) {
            user.setActive(request.getIsActive());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User updatedUser = userRepository.save(user);
        UserResponse response = convertToUserResponse(updatedUser);
        return response;
    }

    // 5. Xóa user (soft delete để tránh foreign key constraint)
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Không tìm thấy user với ID: " + id));

        try {
            // Thử xóa trực tiếp trước
            userRepository.delete(user);
        } catch (Exception e) {
            // Nếu có foreign key constraint, thực hiện soft delete
            user.setActive(false);
            user.setEmail("deleted_" + user.getId() + "_" + user.getEmail()); // Để tránh conflict email khi tạo user mới
            userRepository.save(user);
        }
    }

    // Helper method để convert User entity thành UserResponse
    private UserResponse convertToUserResponse(User user) {
        RoleEnum role = user.getRoles()
                .stream()
                .findFirst()
                .map(r -> RoleEnum.fromDb(r.getName()))
                .orElse(null);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(role)
                .isActive(user.isActive())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
