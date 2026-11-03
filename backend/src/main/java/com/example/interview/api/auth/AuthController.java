package com.example.interview.api.auth;

import com.example.interview.application.auth.AuthService;
import com.example.interview.application.auth.TokenService;
import com.example.interview.domain.user.UserRole;
import com.example.interview.infrastructure.persistence.UserJpaEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Auth endpoints (design §2.2). All under {@code /api/v1/auth/**} → permitAll
 * in the security chain.
 *
 * <ul>
 *   <li>POST /register → 201 {@link AuthResponse.UserDto} | 409 (AC-AUTH-01/02)</li>
 *   <li>POST /login    → 200 {@link AuthResponse.LoginResponse} | 401 | 403 (AC-AUTH-03/04/07)</li>
 *   <li>POST /refresh  → 200 {@link AuthResponse.RefreshResponse} | 401 (AC-AUTH-05/06)</li>
 *   <li>POST /logout   → 204</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    /** AC-AUTH-01 / AC-AUTH-02. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse.UserDto> register(@Valid @RequestBody AuthRequest.RegisterRequest request) {
        UserJpaEntity user = authService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.UserDto.from(user));
    }

    /** AC-AUTH-03 / AC-AUTH-04 / AC-AUTH-07. */
    @PostMapping("/login")
    public AuthResponse.LoginResponse login(@Valid @RequestBody AuthRequest.LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.email(), request.password());
        UserRole role = UserRole.valueOf(result.user().getRole());
        String accessToken = tokenService.generateAccessToken(result.user().getId(), role);
        return new AuthResponse.LoginResponse(
                accessToken,
                result.rawRefreshToken(),
                tokenService.getAccessTtlSeconds()
        );
    }

    /** AC-AUTH-05 / AC-AUTH-06. */
    @PostMapping("/refresh")
    public AuthResponse.RefreshResponse refresh(@Valid @RequestBody AuthRequest.RefreshRequest request) {
        UUID userId = authService.refresh(request.refreshToken());
        // The user's role is re-read from DB to stay correct even if it changed
        // after the access token was issued.
        UserJpaEntity user = authService.getUserById(userId);
        UserRole role = UserRole.valueOf(user.getRole());
        String accessToken = tokenService.generateAccessToken(userId, role);
        return new AuthResponse.RefreshResponse(accessToken, tokenService.getAccessTtlSeconds());
    }

    /** Logout → 204 (client deletes its tokens; server revokes the refresh token). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody AuthRequest.LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
