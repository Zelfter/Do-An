package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for documents table (tài liệu ôn tập).
 * FR-33: Admin CRUD + trigger RAG indexing.
 * FR-28: from weak topic → open study material.
 * AC-DOC-01: only INDEXED documents appear in study materials.
 * AC-DOC-02: INDEX_FAILED stored here; Java never throws error to Admin.
 */
@Entity
@Table(name = "documents")
public class StudyDocumentJpaEntity {

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

    @Column(name = "title", nullable = false, length = 400)
    private String title;

    /** Markdown content — max 100KB enforced at service layer (design.md §6). */
    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMd;

    /**
     * DRAFT | INDEXED | INDEX_FAILED.
     * Java sets this based on Python AI Service response (AC-DOC-01, AC-DOC-02).
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    /** Error message from last failed indexing attempt (AC-DOC-02). */
    @Column(name = "index_error", columnDefinition = "TEXT")
    private String indexError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // ---- Getters / Setters ----

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public TopicJpaEntity getTopic() { return topic; }
    public void setTopic(TopicJpaEntity topic) { this.topic = topic; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContentMd() { return contentMd; }
    public void setContentMd(String contentMd) { this.contentMd = contentMd; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIndexError() { return indexError; }
    public void setIndexError(String indexError) { this.indexError = indexError; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
