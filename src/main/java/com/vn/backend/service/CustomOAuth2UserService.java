package com.vn.backend.service;

import com.vn.backend.model.Role;
import com.vn.backend.model.User;
import com.vn.backend.repository.RoleRepository;
import com.vn.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Trích xuất field chuẩn từ Google
        String email = (String) oAuth2User.getAttributes().get("email");
        Boolean emailVerified = (Boolean) oAuth2User.getAttributes().getOrDefault("email_verified", true);
        String name = (String) oAuth2User.getAttributes().getOrDefault("name", email);
        String picture = (String) oAuth2User.getAttributes().get("picture");
        String sub = (String) oAuth2User.getAttributes().get("sub"); // google user id

        if (email == null || !emailVerified) {
            throw new OAuth2AuthenticationException("Email is missing or not verified");
        }

        // Tìm hoặc tạo user local
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Role role = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Role USER not found"));
            User newUser = User.builder()
                    .email(email)
                    .password("{noop}") // không dùng mật khẩu local
                    .fullName(name)
                    .avatarUrl(picture)
                    .googleId(sub)
                    .isActive(true)
                    .isLocked(false)
                    .mfaEnabled(false)
                    .otpConsumed(true)
                    .roles(Set.of(role))
                    .build();
            return userRepository.save(newUser);
        });

        // (tuỳ chọn) cập nhật avatar/name mỗi lần login
        user.setFullName(name);
        user.setAvatarUrl(picture);
        userRepository.save(user);

        // Trả về DefaultOAuth2User với authorities là ROLE_*
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
                .collect(Collectors.toSet());

        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "sub");
    }
}

