package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UserJpaEntity} (table {@code users}).
 *
 * <p>Used by the auth/user-management application services (Task 03/04). The
 * {@code role} / {@code status} columns are stored as strings; conversion to
 * {@link com.example.interview.domain.user.UserRole} /
 * {@link com.example.interview.domain.user.UserStatus} happens in the service layer.
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    /** FR-01: email uniqueness / lookup by email. */
    Optional<UserJpaEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // ---- FR-34: Admin list/filter users by role and/or status ----

    /** List users whose role matches (case-insensitive). */
    java.util.List<UserJpaEntity> findByRoleIgnoreCase(String role);

    /** List users whose status matches (case-insensitive). */
    java.util.List<UserJpaEntity> findByStatusIgnoreCase(String status);

    /** List users matching both role and status (case-insensitive). */
    java.util.List<UserJpaEntity> findByRoleIgnoreCaseAndStatusIgnoreCase(String role, String status);
}
