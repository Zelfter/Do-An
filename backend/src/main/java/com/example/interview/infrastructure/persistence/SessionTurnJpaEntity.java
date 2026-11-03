package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for session_turns table.
 * FR-16 (chat interface), FR-22 (no duplicate questions), FR-23 (transcript).
 * BR-17: answer_text immutable once set.
 * BR-18: UNGRADED status when AI fails after retries.
 */
@Entity
@Table(name = "session_turns")
public class SessionTurnJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private InterviewSessionJpaEntity session;

    /** NULL for ORIGINAL; points to parent turn for FOLLOW_UP (FR-20). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_turn_id")
    private SessionTurnJpaEntity parentTurn;

    /** NULL for follow-up turns (no entry in questions table). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuestionJpaEntity question;

    /** ORIGINAL | FOLLOW_UP — FR-18, FR-20. */
    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    /** Exact text shown to candidate (stored for transcript FR-23). */
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /** BR-17: set once; never updated. */
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    /** PENDING | ANSWERED | SKIPPED | TIMED_OUT | UNGRADED — BR-18. */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Getters / Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public InterviewSessionJpaEntity getSession() { return session; }
    public void setSession(InterviewSessionJpaEntity s) { this.session = s; }
    public SessionTurnJpaEntity getParentTurn() { return parentTurn; }
    public void setParentTurn(SessionTurnJpaEntity p) { this.parentTurn = p; }
    public QuestionJpaEntity getQuestion() { return question; }
    public void setQuestion(QuestionJpaEntity q) { this.question = q; }
    public String getKind() { return kind; }
    public void setKind(String k) { this.kind = k; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String t) { this.questionText = t; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String a) { this.answerText = a; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public Instant getCreatedAt() { return createdAt; }
}
