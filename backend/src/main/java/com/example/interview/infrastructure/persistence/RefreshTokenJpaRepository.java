package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link RefreshTokenJpaEntity} (table {@code refresh_tokens}).
 *
 * <p>Refresh tokens are stored as a SHA-256 hash of the raw opaque token
 * (design.md §2.1). The raw token is only ever sent to the client once.
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    /** AC-AUTH-05/06: look up a refresh token by its SHA-256 hash. */
    RefreshTokenJpaEntity findByTokenHash(String tokenHash);

    /** Lookup all tokens for a user (used to simulate expiry in tests). */
    java.util.List<RefreshTokenJpaEntity> findByUser_Id(java.util.UUID userId);

    /**
     * FR-34 / AC-USER-01: revoke every refresh token belonging to a user when
     * their account is locked. Bulk update for efficiency.
     */
    @Modifying
    @Query("update RefreshTokenJpaEntity r set r.revoked = true where r.user.id = :userId")
    int revokeAllByUserId(@Param("userId") UUID userId);
}
