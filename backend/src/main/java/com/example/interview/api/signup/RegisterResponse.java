package com.example.interview.api.signup;

import com.example.interview.infrastructure.persistence.UserJpaEntity;

import java.util.UUID;

/**
 * Response body for POST /api/v1/auth/signup → 201 Created.
 *
 * <p>Deliberately excludes {@code password_hash} (NFR-17, AC-AUTH-01 — "password
 * không xuất hiện trong response"). Only the safe user projection is returned.
 */
public record RegisterResponse(UUID id, String email, String role, String status) {

    public static RegisterResponse from(UserJpaEntity user) {
        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus());
    }
}
