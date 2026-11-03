package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for scoring_settings table.
 * NFR-29: ALL thresholds and weights stored here — never hard-coded.
 * BR-01 (rubric weights), BR-03 (score thresholds), BR-05 (follow-up config),
 * BR-14 (weak topic threshold), BR-13 (total score classification).
 *
 * Value is stored as TEXT representing JSONB — parsed by application layer.
 * In Postgres: JSONB. In H2 (tests): TEXT (compatible).
 */
@Entity
@Table(name = "scoring_settings")
public class ScoringSettingsJpaEntity {

    @Id
    @Column(name = "key", nullable = false, length = 100, updatable = false)
    private String key;

    /**
     * JSON value — stored as TEXT to be compatible with both H2 (tests) and PostgreSQL.
     * Application parses JSON at read time. (NFR-29)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value", nullable = false, columnDefinition = "jsonb")
    private String value;

    /** NULL until first Admin update. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserJpaEntity updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // ---- Getters / Setters ----

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public UserJpaEntity getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UserJpaEntity updatedBy) { this.updatedBy = updatedBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
