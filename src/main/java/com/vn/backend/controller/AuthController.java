package com.vn.backend.controller;

import com.vn.backend.dto.request.*;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.LoginResponse;
import com.vn.backend.dto.response.RefreshTokenResponse;
import com.vn.backend.service.AuthService;
import com.vn.backend.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        ApiResponse<LoginResponse> response = authService.login(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody @Valid RegisterRequest request) {
        ApiResponse<?> response = authService.register(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(@RequestHeader(value = "Authorization", required = false) String jwt) {
        ApiResponse<?> response = authService.logout(jwt);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(@RequestBody RefreshTokenRequest request) {
        ApiResponse<RefreshTokenResponse> res = authService.refresh(request);
        return ResponseEntity.status(res.getStatusCode()).body(res);
    }

    @GetMapping("/google")
    public RedirectView startGoogleLogin() {
        return new RedirectView("/oauth2/authorization/google");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        ApiResponse<?> res = authService.fotgotPassword(req);
        return ResponseEntity.status(res.getStatusCode()).body(res);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody ResetPasswordRequest req) {
        ApiResponse<?> res = authService.resetPassword(req);
        return ResponseEntity.status(res.getStatusCode()).body(res);
    }

}
