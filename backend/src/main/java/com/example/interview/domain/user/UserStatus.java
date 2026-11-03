package com.example.interview.domain.user;

/**
 * Account lifecycle status — FR-34, AC-AUTH-07.
 *
 * <p>Stored as a plain string in the {@code users.status} column (VARCHAR(20)).
 * A {@code LOCKED} user cannot log in (HTTP 403) and has all refresh tokens
 * revoked immediately (AC-USER-01).
 */
public enum UserStatus {
    ACTIVE,
    LOCKED
}
