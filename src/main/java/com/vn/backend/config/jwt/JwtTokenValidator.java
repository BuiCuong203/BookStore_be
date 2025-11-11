package com.vn.backend.config.jwt;

import com.vn.backend.exception.AppException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtTokenValidator extends OncePerRequestFilter {
    @Value("${jwt.signerKey}")
    private String secretKey;

    @Autowired
    private JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (jwt != null && jwtProvider.getClaimsFromToken(jwt) != null) {
                Claims claims = jwtProvider.getClaimsFromToken(jwt);

                String email = claims.get("email", String.class);
                List<String> rolesList = claims.get("roles", List.class);
                List<String> permsList = claims.get("perms", List.class);

                List<GrantedAuthority> authorities = new ArrayList<>();

                if (rolesList != null && !rolesList.isEmpty()) {
                    authorities.addAll(rolesList.stream()
                            .map(role -> new SimpleGrantedAuthority(role))
                            .collect(Collectors.toList()));
                }

                if (permsList != null && !permsList.isEmpty()) {
                    authorities.addAll(permsList.stream()
                            .map(perm -> new SimpleGrantedAuthority(perm))
                            .collect(Collectors.toList())
                    );
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Token not found", e);
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
