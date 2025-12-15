package com.vn.backend.config.jwt;

import com.vn.backend.exception.AppException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class JwtProvider {
    @Value("${jwt.accessExpMs}")
    private long accessExpMs;

    @Value("${jwt.refreshExpMs}")
    private long refreshExpMs;

    @Value("${jwt.signerKey}")
    private String secretKey;

    public String generateAccessToken(Authentication auth) {
        String email = auth.getName();

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .distinct()
                .toList();

        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());

        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpMs);

        return Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("email", email)
                .claim("roles", roles)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String email) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpMs);

        return Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("email", email)
                .signWith(key)
                .compact();
    }

    public String getEmailFromJwtToken(String jwt) {
        Claims claims = getClaimsFromToken(jwt);
        return claims.get("email", String.class);
    }

    public LocalDateTime getExpiredAt(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public Claims getClaimsFromToken(String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
            return Jwts.parser().setSigningKey(key).build().parseClaimsJws(token).getBody();

        } catch (ExpiredJwtException e) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), "Mã thông báo đã hết hạn", e);
        } catch (SignatureException | SecurityException e) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), "Chữ ký mã thông báo không hợp lệ", e);
        } catch (MalformedJwtException e) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), "Định dạng mã thông báo sai", e);
        } catch (UnsupportedJwtException e) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), "Mã thông báo không được hỗ trợ", e);
        } catch (IllegalArgumentException e) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Mã thông báo không hợp lệ", e);
        }
    }
}
