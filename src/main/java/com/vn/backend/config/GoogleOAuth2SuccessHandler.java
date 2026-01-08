package com.vn.backend.config;

import com.vn.backend.config.jwt.JwtProvider;
import com.vn.backend.model.RefreshToken;
import com.vn.backend.model.User;
import com.vn.backend.repository.RefreshTokenRepository;
import com.vn.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GoogleOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse resp,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauth2Auth = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauth2Auth.getPrincipal();
        String email = (String) principal.getAttributes().get("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

        // Tạo Authentication “chuẩn” để tái dùng generateAccessToken(Authentication)
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
                .map(GrantedAuthority.class::cast)
                .toList();
        Authentication authForJwt = new UsernamePasswordAuthenticationToken(email, null, authorities);

        String accessToken = jwtProvider.generateAccessToken(authForJwt);
        String refreshToken = jwtProvider.generateRefreshToken(email);
        String role = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst().orElse(null);

        // Lưu refresh token vào DB (whitelist)
        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshToken)
                .userId(user.getId())
                .expiredAt(LocalDateTime.now().plusSeconds( /* = jwt.refreshExpMs/1000 */ 1209600))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build());

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/auth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("role", role)
                .build().toUriString();
        getRedirectStrategy().sendRedirect(req, resp, targetUrl);
    }
}

