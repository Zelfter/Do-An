package com.example.interview.domain.user;

/**
 * Application roles — NFR-19, FR-RBAC.
 *
 * <p>Stored as a plain string in the {@code users.role} column (VARCHAR(20)).
 * Spring Security authorities are derived from this enum as {@code ROLE_CANDIDATE}
 * / {@code ROLE_ADMIN}. A {@code RoleHierarchy} makes ADMIN imply CANDIDATE so
 * admins can also view candidate-facing screens (FR-RBAC).
 */
public enum UserRole {
    CANDIDATE,
    ADMIN
}
