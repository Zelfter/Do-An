package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for topics table.
 * FR-37: Admin manages topic catalog and aliases.
 * NFR-27: translations in TopicTranslationJpaEntity (language as content attribute).
 * BR-07: topic.code used as stable key for JD mapping.
 */
@Entity
@Table(name = "topics")
public class TopicJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Unique snake_case code — immutable after creation.
     * Used as stable external reference in JD analysis (FR-06, BR-07).
     */
    @Column(name = "code", nullable = false, unique = true, length = 64, updatable = false)
    private String code;

    /** Self-referencing parent for 1-level hierarchy (FR-37). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TopicJpaEntity parent;

    /**
     * Soft-delete flag.
     * AC-TOPIC-02: cannot hard-delete topic with linked questions/documents.
     * AC-TOPIC-03: deactivate instead.
     */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** NFR-27: i18n translations (vi required; en for future). */
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TopicTranslationJpaEntity> translations = new ArrayList<>();

    /** FR-37: keyword aliases for JD → topic mapping. */
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TopicAliasJpaEntity> aliases = new ArrayList<>();

    // ---- Getters / Setters ----

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public TopicJpaEntity getParent() { return parent; }
    public void setParent(TopicJpaEntity parent) { this.parent = parent; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }

    public List<TopicTranslationJpaEntity> getTranslations() { return translations; }
    public void setTranslations(List<TopicTranslationJpaEntity> translations) { this.translations = translations; }

    public List<TopicAliasJpaEntity> getAliases() { return aliases; }
    public void setAliases(List<TopicAliasJpaEntity> aliases) { this.aliases = aliases; }
}
