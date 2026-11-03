package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for ai_call_logs table.
 * NFR-24: log every call to Python AI Service.
 * AC-LOG-01: every /ai/v1/index/* call produces a log entry.
 */
@Entity
@Table(name = "ai_call_logs")
public class AiCallLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** Set during interview turns; NULL for content management calls. */
    @Column(name = "session_id")
    private UUID sessionId;

    /** Set during interview turns; NULL for indexing calls. */
    @Column(name = "turn_id")
    private UUID turnId;

    /**
     * Purpose identifier — e.g.:
     * INDEX_QUESTION, INDEX_DOCUMENT, EVALUATE_TURN, JD_ANALYZE, GENERATE_REPORT.
     */
    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose;

    /** Provider name: gemini, groq, mock. */
    @Column(name = "provider", length = 50)
    private String provider;

    /** Model identifier. */
    @Column(name = "model", length = 100)
    private String model;

    /** SUCCESS | FAILED | TIMEOUT. */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** Wall-clock time in milliseconds from Java perspective. */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    /** Error message when status != SUCCESS. */
    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // ---- Getters / Setters ----

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public UUID getTurnId() { return turnId; }
    public void setTurnId(UUID turnId) { this.turnId = turnId; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Instant getCreatedAt() { return createdAt; }
}
