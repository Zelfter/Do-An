package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for users table.
 * FR-01 (Auth), FR-RBAC (roles), FR-34 (user management).
 * NFR-17: password_hash stores bcrypt output — never plaintext.
 *
 * Design note: domain model User.java will be added in Task 03 (Auth).
 * This entity class is created in Task 02 to satisfy Flyway V001 + JPA schema validation.
 */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** RFC 5322 email, unique, max 254 chars (FR-01). */
    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    /** BCrypt hash cost≥12 — NFR-17. Never expose in responses. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** CANDIDATE or ADMIN — NFR-19, FR-RBAC. */
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    /** ACTIVE or LOCKED — FR-34. */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ---- Getters / Setters ----

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
