package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for turn_evaluations table.
 * BR-01: 4-criterion rubric; Java calculates total_score from LLM's criterion values.
 * BR-20: feedback null during session; populated when report is generated.
 * AC-05: stored but not returned in any API while session is running.
 */
@Entity
@Table(name = "turn_evaluations")
public class TurnEvaluationJpaEntity {

    /** PK = FK to session_turns. One evaluation per turn. */
    @Id
    @Column(name = "turn_id", updatable = false, nullable = false)
    private UUID turnId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "turn_id")
    private SessionTurnJpaEntity turn;

    /** BR-01: accuracy criterion score (0–10 scale, then weighted ×0.50). */
    @Column(name = "accuracy", nullable = false, precision = 4, scale = 2)
    private BigDecimal accuracy;

    /** BR-01: completeness criterion (weighted ×0.30). */
    @Column(name = "completeness", nullable = false, precision = 4, scale = 2)
    private BigDecimal completeness;

    /** BR-01: clarity criterion (weighted ×0.10). */
    @Column(name = "clarity", nullable = false, precision = 4, scale = 2)
    private BigDecimal clarity;

    /** BR-01: example/practical criterion (weighted ×0.10). */
    @Column(name = "example_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal exampleScore;

    /** Weighted total computed by Java — never by LLM (NFR-20). */
    @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore;

    /** JSON array of missed key_point IDs — used in report (FR-26). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missed_key_points", nullable = false, columnDefinition = "jsonb")
    private String missedKeyPoints = "[]";

    /** FR-26: LLM-generated per-turn feedback; null until report is created (BR-20). */
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "graded_at", nullable = false, updatable = false)
    private Instant gradedAt = Instant.now();

    // Getters / Setters
    public UUID getTurnId() { return turnId; }
    public SessionTurnJpaEntity getTurn() { return turn; }
    public void setTurn(SessionTurnJpaEntity t) { this.turn = t; }
    public BigDecimal getAccuracy() { return accuracy; }
    public void setAccuracy(BigDecimal a) { this.accuracy = a; }
    public BigDecimal getCompleteness() { return completeness; }
    public void setCompleteness(BigDecimal c) { this.completeness = c; }
    public BigDecimal getClarity() { return clarity; }
    public void setClarity(BigDecimal c) { this.clarity = c; }
    public BigDecimal getExampleScore() { return exampleScore; }
    public void setExampleScore(BigDecimal e) { this.exampleScore = e; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal t) { this.totalScore = t; }
    public String getMissedKeyPoints() { return missedKeyPoints; }
    public void setMissedKeyPoints(String m) { this.missedKeyPoints = m; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String f) { this.feedback = f; }
    public Instant getGradedAt() { return gradedAt; }
}
