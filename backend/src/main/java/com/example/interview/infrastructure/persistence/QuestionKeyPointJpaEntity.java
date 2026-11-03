package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * JPA entity for question_key_points table.
 * BR-02: key_points used as grounding context for LLM scoring (NFR-15).
 * BR-19: at least 1 key_point required before question can be published.
 * FR-26: missed key_points reported per turn in the session report.
 */
@Entity
@Table(name = "question_key_points",
       uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "seq"}))
public class QuestionKeyPointJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, updatable = false)
    private QuestionJpaEntity question;

    /** Display order — starts at 1. */
    @Column(name = "seq", nullable = false)
    private int seq;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // ---- Getters / Setters ----

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public QuestionJpaEntity getQuestion() { return question; }
    public void setQuestion(QuestionJpaEntity question) { this.question = question; }

    public int getSeq() { return seq; }
    public void setSeq(int seq) { this.seq = seq; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
