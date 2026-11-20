package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for questions table.
 * FR-32: Admin CRUD questions with reference_answer and key_points.
 * BR-02: reference_answer + key_points required for scoring.
 * BR-19: publish requires all mandatory fields.
 * AC-18: publish without required fields → HTTP 422.
 * AC-19: DRAFT questions excluded from RAG pool.
 */
@Entity
@Table(name = "questions")
public class QuestionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private TopicJpaEntity topic;

    /** Default 'vi' — NFR-26, NFR-27. */
    @Column(name = "language", nullable = false, length = 8)
    private String language = "vi";

    /** JUNIOR | MID | SENIOR — BR-08 (difficulty). */
    @Column(name = "level", nullable = false, length = 20)
    private String level;

    /** Question text — max 5000 chars enforced at service layer (design.md §6). */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** BR-02: required before publish; validated in QuestionPublishValidator. */
    @Column(name = "reference_answer", nullable = false, columnDefinition = "TEXT")
    private String referenceAnswer = "";

    /** Optional scenario-specific follow-up question used by the interview flow. */
    @Column(name = "follow_up_question", columnDefinition = "TEXT")
    private String followUpQuestion;

    /** DRAFT | PUBLISHED | ARCHIVED — BR-19, AC-18. */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    /**
     * NOT_INDEXED | INDEXING | INDEXED | INDEX_FAILED
     * INDEX_FAILED questions excluded from RAG pool even if PUBLISHED (AC-19).
     */
    @Column(name = "index_status", nullable = false, length = 20)
    private String indexStatus = "NOT_INDEXED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /** BR-19: at least 1 key_point required before publish. */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("seq ASC")
    private List<QuestionKeyPointJpaEntity> keyPoints = new ArrayList<>();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // ---- Getters / Setters ----

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public TopicJpaEntity getTopic() { return topic; }
    public void setTopic(TopicJpaEntity topic) { this.topic = topic; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReferenceAnswer() { return referenceAnswer; }
    public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }

    public String getFollowUpQuestion() { return followUpQuestion; }
    public void setFollowUpQuestion(String followUpQuestion) { this.followUpQuestion = followUpQuestion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIndexStatus() { return indexStatus; }
    public void setIndexStatus(String indexStatus) { this.indexStatus = indexStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public List<QuestionKeyPointJpaEntity> getKeyPoints() { return keyPoints; }
    public void setKeyPoints(List<QuestionKeyPointJpaEntity> keyPoints) { this.keyPoints = keyPoints; }
}
