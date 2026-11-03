package com.example.interview.api.auth;

import com.example.interview.application.auth.AuthService;
import com.example.interview.infrastructure.persistence.UserJpaEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * GET /api/v1/me → 200 {@link AuthResponse.UserDto} (design §2.2).
 *
 * <p>Requires an authenticated JWT (matched by the {@code /api/v1/**} → authenticated
 * rule). The principal is the user id set by {@link com.example.interview.infrastructure.security.JwtAuthFilter}.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final AuthService authService;

    public MeController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public AuthResponse.UserDto me(@AuthenticationPrincipal UUID userId) {
        UserJpaEntity user = authService.getUserById(userId);
        return AuthResponse.UserDto.from(user);
    }
}
