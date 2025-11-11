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
import java.util.Date;
import java.util.List;

@Service
public class JwtProvider {
    @Value("${jwt.signerKey}")
    private String secretKey;

    public String generateToken(Authentication auth) {
        String email = auth.getName();
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .distinct()
                .toList();

        List<String> perms = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .distinct()
                .toList();

        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());

        String jwt = Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + 300000))
                .claim("email", email)
                .claim("roles", roles)
                .claim("perms", perms)
                .signWith(key)
                .compact();

        return jwt;
    }

    public String getEmailFromJwtToken(String jwt) {
        Claims claims = getClaimsFromToken(jwt);
        String email = String.valueOf(claims.get("email"));

        return email;
    }

    public Claims getClaimsFromToken(String jwt) {
        try {
            if (jwt.startsWith("Bearer ")) {
                jwt = jwt.substring(7);
            }

            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
            Claims claims = Jwts.parser().setSigningKey(key).build().parseClaimsJws(jwt).getBody();

            return claims;

        } catch (ExpiredJwtException e) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Token has expired", e);
        } catch (SignatureException | SecurityException e) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid token signature", e);
        } catch (MalformedJwtException e) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Token format is wrong", e);
        } catch (UnsupportedJwtException e) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Token not supported", e);
        } catch (IllegalArgumentException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid token", e);
        }
    }
}
