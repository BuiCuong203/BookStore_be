package com.vn.backend.config;

import com.vn.backend.config.jwt.JwtTokenValidator;
import com.vn.backend.service.CustomOAuth2UserService;
import com.vn.backend.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtTokenValidator jwtTokenValidator;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    // Bean: resolver chèn prompt=select_account
    @Bean
    public OAuth2AuthorizationRequestResolver googlePromptSelectAccountResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        return new PromptSelectAccountResolver(clientRegistrationRepository);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, OAuth2AuthorizationRequestResolver googlePromptSelectAccountResolver) throws Exception {
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/seller/**").hasRole("SELLER")
                        .anyRequest().permitAll());
        http.authenticationProvider(authenticationProvider());
        http.csrf(csrf -> csrf.disable());
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.addFilterBefore(jwtTokenValidator, UsernamePasswordAuthenticationFilter.class);
        http.oauth2Login(oauth -> oauth
                .authorizationEndpoint(authz -> authz
                        // Gắn resolver để luôn bật hộp chọn tài khoản
                        .authorizationRequestResolver(googlePromptSelectAccountResolver)
                )
                .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                .successHandler(googleOAuth2SuccessHandler)   // trả JWT & refresh cho FE
                .failureHandler((req, resp, ex) -> {
                    resp.setStatus(401);
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.getWriter().write("{\"statusCode\":401,\"message\":\"Google login failed\"}");
                })
        );


        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration cfg = new CorsConfiguration();
                cfg.addAllowedOrigin(allowedOrigin);
                cfg.setAllowedMethods(Collections.singletonList("*"));
                cfg.setAllowedHeaders(Collections.singletonList("*"));
                cfg.setExposedHeaders(Collections.singletonList("Authorization"));
                cfg.setAllowCredentials(true);
                cfg.setMaxAge(3600L);
                return cfg;
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Resolver nội bộ: delegate cho DefaultOAuth2AuthorizationRequestResolver
     * và chèn thêm prompt=select_account (hoặc "consent select_account")
     */
    static class PromptSelectAccountResolver implements OAuth2AuthorizationRequestResolver {
        private static final String AUTHORIZATION_REQUEST_BASE_URI = "/oauth2/authorization";
        private final DefaultOAuth2AuthorizationRequestResolver delegate;

        PromptSelectAccountResolver(ClientRegistrationRepository clientRegistrationRepository) {
            this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                    clientRegistrationRepository, AUTHORIZATION_REQUEST_BASE_URI
            );
        }

        @Override
        public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
            OAuth2AuthorizationRequest req = delegate.resolve(request);
            return customize(req);
        }

        @Override
        public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
            OAuth2AuthorizationRequest req = delegate.resolve(request, clientRegistrationId);
            return customize(req);
        }

        private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest req) {
            if (req == null) return null;

            Map<String, Object> extra = new HashMap<>(req.getAdditionalParameters());
            // Chọn một:
            // extra.put("prompt", "select_account");
            extra.put("prompt", "consent select_account"); // ép consent + chooser

            return OAuth2AuthorizationRequest.from(req)
                    .additionalParameters(extra)
                    .build();
        }
    }
}
