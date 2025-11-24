package com.vn.backend.config.jwt;

import com.vn.backend.exception.AppException;
import com.vn.backend.repository.InvalidTokenRepository;
import com.vn.backend.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtTokenValidator extends OncePerRequestFilter {
    @Value("${jwt.signerKey}")
    private String secretKey;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private InvalidTokenRepository invalidTokenRepository;

    private boolean isTokenBlacklisted(String token) {
        return invalidTokenRepository.findById(token).isPresent();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwt = getJwtFromRequest(request);

        // Không có token → bỏ qua, để các filter khác xử lý (public API vẫn chạy được)
        if (!StringUtils.hasText(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isTokenBlacklisted(jwt)) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), "Invalid token");
        }

        Claims claims = jwtProvider.getClaimsFromToken(jwt);
        String email = claims.get("email", String.class);

        // Chỉ set auth nếu chưa có trong context
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Nạp lại UserDetails từ DB (bao gồm roles + permissions)
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
