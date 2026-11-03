package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * JPA entity for topic_aliases table.
 * FR-37: keyword synonyms for JD → topic mapping.
 * BR-07: scope of questions follows JD topics via aliases.
 */
@Entity
@Table(name = "topic_aliases",
       uniqueConstraints = @UniqueConstraint(columnNames = {"topic_id", "alias"}))
public class TopicAliasJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false, updatable = false)
    private TopicJpaEntity topic;

    /** Alias string, e.g. "OOP", "Lập trình hướng đối tượng". */
    @Column(name = "alias", nullable = false, length = 200)
    private String alias;

    // ---- Getters / Setters ----

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public TopicJpaEntity getTopic() { return topic; }
    public void setTopic(TopicJpaEntity topic) { this.topic = topic; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
}
