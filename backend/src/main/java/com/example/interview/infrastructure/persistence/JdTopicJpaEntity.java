package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * JPA entity for jd_topics table.
 * FR-06: JD analysis maps raw terms to system topics.
 * FR-07: candidate can toggle topics before starting session.
 * FR-38: unsupported terms (topic_id NULL) shown to candidate.
 */
@Entity
@Table(name = "jd_topics")
public class JdTopicJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jd_id", nullable = false, updatable = false)
    private JobDescriptionJpaEntity jobDescription;

    /** NULL when term not found in topic catalog (FR-38: unsupported term). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private TopicJpaEntity topic;

    @Column(name = "raw_term", nullable = false, length = 300)
    private String rawTerm;

    /** REQUIRED | PREFERRED | NICE_TO_HAVE — from JD analysis. */
    @Column(name = "importance", length = 20)
    private String importance;

    /** Candidate can toggle (FR-07). */
    @Column(name = "is_selected", nullable = false)
    private boolean selected = true;

    // Getters / Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public JobDescriptionJpaEntity getJobDescription() { return jobDescription; }
    public void setJobDescription(JobDescriptionJpaEntity jobDescription) { this.jobDescription = jobDescription; }
    public TopicJpaEntity getTopic() { return topic; }
    public void setTopic(TopicJpaEntity topic) { this.topic = topic; }
    public String getRawTerm() { return rawTerm; }
    public void setRawTerm(String rawTerm) { this.rawTerm = rawTerm; }
    public String getImportance() { return importance; }
    public void setImportance(String importance) { this.importance = importance; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
