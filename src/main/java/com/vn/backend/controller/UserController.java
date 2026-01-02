package com.vn.backend.controller;

import com.vn.backend.dto.request.CreateUserRequest;
import com.vn.backend.dto.request.UpdateUserRequest;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.PagedResponse;
import com.vn.backend.dto.response.UserResponse;
import com.vn.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    @Autowired
    private UserService userService;

    // 1. GET /api/users - Lấy danh sách users với phân trang và tìm kiếm
    @GetMapping
    public ApiResponse<PagedResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            Pageable pageable) {
        return ApiResponse.<PagedResponse<UserResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .data(userService.getAllUsers(keyword, pageable, activeOnly))
                .message("Lấy danh sách users thành công")
                .build();
    }

    // 2. GET /api/users/{id} - Lấy thông tin user theo ID
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.<UserResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .data(userService.getUserById(id))
                .message("Lấy thông tin user thành công")
                .build();
    }

    // 3. POST /api/users - Tạo user mới (Admin only)
    @PostMapping
    public ApiResponse<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        return ApiResponse.<UserResponse>builder()
                .statusCode(HttpStatus.CREATED.value())
                .data(userService.createUser(request))
                .message("Tạo user mới thành công")
                .build();
    }

    // 4. PUT /api/users/{id} - Cập nhật thông tin user
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return ApiResponse.<UserResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .data(userService.updateUser(id, request))
                .message("Cập nhật user thành công")
                .build();
    }

    // 5. DELETE /api/users/{id} - Xóa user (Admin only)
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.<String>builder().message("Xóa user thành công").build();
    }
}
