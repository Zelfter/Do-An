package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for session_reports table.
 * FR-43: async report — GENERATING → READY or PARTIAL.
 * AC-22: PARTIAL report still shows scores and missed key_points even if LLM prose fails.
 * BR-20: report only accessible after session ends.
 */
@Entity
@Table(name = "session_reports")
public class SessionReportJpaEntity {

    @Id
    @Column(name = "session_id", updatable = false, nullable = false)
    private UUID sessionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "session_id")
    private InterviewSessionJpaEntity session;

    /** GENERATING | READY | PARTIAL — FR-43, AC-22. */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "GENERATING";

    /** LLM-generated overall summary; null when GENERATING or if LLM failed (PARTIAL). */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "generated_at")
    private Instant generatedAt;

    // Getters / Setters
    public UUID getSessionId() { return sessionId; }
    public InterviewSessionJpaEntity getSession() { return session; }
    public void setSession(InterviewSessionJpaEntity s) { this.session = s; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant t) { this.generatedAt = t; }
}
