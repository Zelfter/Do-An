package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for interview_sessions table.
 * FR-09 (create session), BR-16 (state machine: CREATED→IN_PROGRESS→COMPLETED/TIMED_OUT/CANCELLED).
 * NFR-16: resume session after page reload — elapsed_ms tracked server-side.
 * BR-20: total_score not exposed until session ends.
 */
@Entity
@Table(name = "interview_sessions")
public class InterviewSessionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserJpaEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jd_id", nullable = false, updatable = false)
    private JobDescriptionJpaEntity jobDescription;

    /** CREATED | IN_PROGRESS | COMPLETED | TIMED_OUT | CANCELLED — BR-16. */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "CREATED";

    @Column(name = "time_limit_sec", nullable = false)
    private int timeLimitSec = 3600;

    /** Server-side authoritative timer — NFR-16 resume support. */
    @Column(name = "elapsed_ms", nullable = false)
    private long elapsedMs = 0;

    /**
     * Snapshot of scoring_settings at session creation (NFR-29).
     * Stored as TEXT (JSONB in Postgres, TEXT in H2).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_snapshot", nullable = false, columnDefinition = "jsonb")
    private String configSnapshot = "{}";

    /** Calculated after session ends; NULL while IN_PROGRESS (BR-20). */
    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    /**
     * NFR-16: wall-clock time when the current answering window started.
     * Null while the AI is grading (timer paused) or after the session has ended.
     */
    @Column(name = "resume_at")
    private Instant resumeAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // Getters / Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UserJpaEntity getUser() { return user; }
    public void setUser(UserJpaEntity user) { this.user = user; }
    public JobDescriptionJpaEntity getJobDescription() { return jobDescription; }
    public void setJobDescription(JobDescriptionJpaEntity jd) { this.jobDescription = jd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTimeLimitSec() { return timeLimitSec; }
    public void setTimeLimitSec(int t) { this.timeLimitSec = t; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long e) { this.elapsedMs = e; }
    public String getConfigSnapshot() { return configSnapshot; }
    public void setConfigSnapshot(String s) { this.configSnapshot = s; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal s) { this.totalScore = s; }
    public Instant getResumeAt() { return resumeAt; }
    public void setResumeAt(Instant r) { this.resumeAt = r; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
