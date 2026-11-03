package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for refresh_tokens table.
 * FR-01-JWT: server-side refresh token (single-use, revocable).
 * FR-34: revoking all tokens of a user when they are LOCKED.
 * design.md §2.1: token stored as SHA-256 hash; raw token sent to client once.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** FK to users.id — ON DELETE CASCADE (V001 migration). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserJpaEntity user;

    /** SHA-256 hash of the raw opaque token. Never store raw token. */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /** Token expiry — TTL from JWT_REFRESH_TTL_DAYS env var (default 7 days). */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /**
     * Revoked flag — set TRUE when:
     * - token consumed (single-use, AC-AUTH-05)
     * - user account LOCKED (FR-34, AC-USER-01)
     */
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // ---- Getters / Setters ----

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserJpaEntity getUser() { return user; }
    public void setUser(UserJpaEntity user) { this.user = user; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public Instant getCreatedAt() { return createdAt; }
}
