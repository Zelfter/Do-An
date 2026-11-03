package com.example.interview.infrastructure.security;

import com.example.interview.api.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;

/**
 * Spring Security 6 configuration for the JWT stateless API (design §2.4).
 *
 * <p>Filter chain rules (order matters — most specific first):
 * <pre>
 *   /api/v1/auth/**         → permitAll
 *   /api/v1/system/warmup   → permitAll   (UC-18 warm-up, no JWT)
 *   /api/v1/admin/**        → hasRole(ADMIN)   (FR-RBAC, AC-RBAC-01)
 *   /ai/v1/**               → permitAll   (internal key checked by Python; not JWT)
 *   /api/v1/**              → authenticated (AC-RBAC-02 → 401 without JWT)
 *   anyRequest              → authenticated
 * </pre>
 *
 * <p>RoleHierarchy: ADMIN &gt; CANDIDATE so admins can view candidate screens
 * (FR-RBAC). CSRF is disabled (Bearer-token API, no cookies). Sessions are stateless.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // NFR-17: bcrypt cost 12.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_ADMIN > ROLE_CANDIDATE");
        return hierarchy;
    }

    /** hasRole(ADMIN) rule that also honours the role hierarchy (ADMIN > CANDIDATE). */
    private AuthorizationManager<RequestAuthorizationContext> adminAuthorizationManager(RoleHierarchy roleHierarchy) {
        AuthorityAuthorizationManager<RequestAuthorizationContext> manager =
                AuthorityAuthorizationManager.hasRole("ADMIN");
        manager.setRoleHierarchy(roleHierarchy);
        return manager;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthFilter jwtAuthFilter,
                                           RoleHierarchy roleHierarchy, // forces creation order
                                           ObjectMapper objectMapper)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults()) // uses the CorsConfigurationSource bean below
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/system/warmup").permitAll()
                        .requestMatchers("/ai/v1/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").access(adminAuthorizationManager(roleHierarchy))
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(jsonEntryPoint(objectMapper))
                        .accessDeniedHandler(jsonAccessDeniedHandler(objectMapper))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList());
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Returns JSON 401 for missing/invalid authentication (AC-RBAC-02). */
    private AuthenticationEntryPoint jsonEntryPoint(ObjectMapper objectMapper) {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) -> {
            writeJsonError(response, objectMapper, HttpStatus.UNAUTHORIZED,
                    "Xác thực thất bại: thiếu hoặc không hợp lệ JWT");
        };
    }

    /** Returns JSON 403 for insufficient role (AC-RBAC-01). */
    private AccessDeniedHandler jsonAccessDeniedHandler(ObjectMapper objectMapper) {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) -> {
            writeJsonError(response, objectMapper, HttpStatus.FORBIDDEN,
                    "Không đủ quyền truy cập tài nguyên này");
        };
    }

    private void writeJsonError(HttpServletResponse response, ObjectMapper objectMapper,
                                HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse body = new ErrorResponse(status.value(), message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
