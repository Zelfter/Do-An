package com.example.interview.api.auth;

import com.example.interview.infrastructure.persistence.UserJpaEntity;

import java.util.UUID;

/**
 * Response bodies for the auth + me endpoints (design §2.2).
 *
 * <p><b>None</b> of these records expose a password field or the password hash
 * (NFR-17, AC-AUTH-01 — "password không xuất hiện trong response").
 */
public final class AuthResponse {

    private AuthResponse() {
    }

    /** POST /auth/login → 200. */
    public record LoginResponse(String accessToken, String refreshToken, long expiresIn) {
    }

    /** POST /auth/refresh → 200. */
    public record RefreshResponse(String accessToken, long expiresIn) {
    }

    /**
     * User projection returned by POST /auth/register (201) and GET /api/v1/me (200).
     * Deliberately excludes password_hash.
     */
    public record UserDto(UUID id, String email, String role, String status) {

        public static UserDto from(UserJpaEntity user) {
            return new UserDto(user.getId(), user.getEmail(), user.getRole(), user.getStatus());
        }
    }
}
