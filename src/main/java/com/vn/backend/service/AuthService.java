package com.vn.backend.service;

import com.vn.backend.config.jwt.JwtProvider;
import com.vn.backend.dto.request.LoginRequest;
import com.vn.backend.dto.request.RefreshTokenRequest;
import com.vn.backend.dto.request.RegisterRequest;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.LoginResponse;
import com.vn.backend.dto.response.RefreshTokenResponse;
import com.vn.backend.exception.AppException;
import com.vn.backend.model.InvalidTokens;
import com.vn.backend.model.RefreshToken;
import com.vn.backend.model.Role;
import com.vn.backend.model.User;
import com.vn.backend.repository.InvalidTokenRepository;
import com.vn.backend.repository.RefreshTokenRepository;
import com.vn.backend.repository.RoleRepository;
import com.vn.backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private InvalidTokenRepository invalidTokenRepository;

    public ApiResponse<LoginResponse> login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                );
        try {
            // 1. Thử authenticate
            Authentication authentication = authenticationManager.authenticate(authRequest);

            // 2. Lấy thông tin user thật từ DB
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // 3. Cập nhật lastLoginAt
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // 4. Sinh JWT như cũ
            String accessToken = jwtProvider.generateAccessToken(authentication);
            String refreshToken = jwtProvider.generateRefreshToken(request.getEmail());

            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .findFirst()
                    .orElse(null);

            refreshTokenRepository.save(RefreshToken.builder()
                    .token(refreshToken)
                    .userId(user.getId())
                    .expiredAt(LocalDateTime.now().plusSeconds(getRefreshExpSeconds()))
                    .revoked(false)
                    .createdAt(LocalDateTime.now())
                    .build());

            LoginResponse loginResponse = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .role(role)
                    .build();

            return ApiResponse.<LoginResponse>builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Login successful")
                    .data(loginResponse)
                    .build();
        } catch (UsernameNotFoundException ex) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), "User not found");

        } catch (BadCredentialsException ex) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), "Invalid password");
        }
    }

    public ApiResponse<String> register(RegisterRequest request) {
        try {
            // Kiểm tra email tồn tại
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new AppException(HttpStatus.CONFLICT.value(), "Email already exists");
            }

            // Lấy role USER từ database
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED.value(), "Role USER not found"));

            // Tạo user mới
            User newUser = User.builder()
                    .email(request.getEmail())
                    .fullName(request.getFullName())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .isActive(true)
                    .isLocked(false)
                    .mfaEnabled(false)
                    .otpConsumed(false)
                    .build();

            // Gán role USER
            newUser.getRoles().add(userRole);

            userRepository.save(newUser);

            return ApiResponse.<String>builder()
                    .statusCode(HttpStatus.CREATED.value())
                    .message("User registered successfully")
                    .data(null)
                    .build();

        } catch (RuntimeException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        }

    }

    public ApiResponse<String> logout(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Token is missing");
        }
        jwt = jwt.startsWith("Bearer ") ? jwt.substring(7) : jwt;

        // Lấy thời gian hết hạn token
        LocalDateTime expiredAt = jwtProvider.getExpiredAt(jwt);

        // Lưu vào blacklist
        invalidTokenRepository.save(new InvalidTokens(jwt, expiredAt));

        return ApiResponse.<String>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Logout successful")
                .data(null)
                .build();
    }

    public ApiResponse<RefreshTokenResponse> refresh(RefreshTokenRequest req) {
        String token = req.getRefreshToken();
        if (token == null || token.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Refresh token is missing");
        }

        // 1) Tồn tại trong DB và chưa revoked
        RefreshToken saved = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED.value(), "Invalid refresh token"));

        if (saved.isRevoked()) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), "Refresh token has been revoked");
        }

        // 2) Validate chữ ký + còn hạn
        Claims claims = jwtProvider.getClaimsFromToken(token);
        String email = claims.get("email", String.class);

        // 3) Lấy user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED.value(), "User not found"));

        // 4) Rotation: revoke old, issue new
        saved.setRevoked(true);
        refreshTokenRepository.save(saved);

        // Tạo Authentication tạm cho JwtProvider
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
                .collect(Collectors.toList());
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);

        // Dùng lại generateAccessToken(Authentication auth)
        String newAccess = jwtProvider.generateAccessToken(authentication);
        String newRefresh = jwtProvider.generateRefreshToken(email);

        refreshTokenRepository.save(RefreshToken.builder()
                .token(newRefresh)
                .userId(user.getId())
                .expiredAt(LocalDateTime.now().plusSeconds(getRefreshExpSeconds()))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build());

        String role = user.getRoles().stream()
                .findFirst()
                .map(r -> "ROLE_" + r.getName())
                .orElse(null);

        RefreshTokenResponse data = RefreshTokenResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .role(role)
                .build();

        return ApiResponse.<RefreshTokenResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Refresh successful")
                .data(data)
                .build();

    }

    private long getRefreshExpSeconds() {
        return Long.getLong("jwt.refreshExpMs", 604800000) / 1000L;
    }
}
