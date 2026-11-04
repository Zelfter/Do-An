package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for session_topics (composite PK: session_id + topic_id).
 *
 * <p>BR-10/BR-11: per-topic weights and scores for the session.
 * BR-14: is_weak flag (topic_score &lt; threshold OR &gt;= min weak answers).
 */
@Entity
@Table(name = "session_topics")
@IdClass(SessionTopicJpaEntity.SessionTopicId.class)
public class SessionTopicJpaEntity {

    @Id
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Id
    @Column(name = "topic_id", nullable = false, updatable = false)
    private UUID topicId;

    /** BR-10: topic weight (REQUIRED skills weighted higher — score_weights config). */
    @Column(name = "weight", nullable = false, precision = 4, scale = 2)
    private BigDecimal weight = new BigDecimal("1.00");

    /** BR-11: aggregate score for the topic (0–10); null until finalised. */
    @Column(name = "topic_score", precision = 5, scale = 2)
    private BigDecimal topicScore;

    /** BR-14: weak-topic flag; null until finalised. */
    @Column(name = "is_weak")
    private Boolean isWeak;

    // ---- Getters / Setters ----

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public UUID getTopicId() { return topicId; }
    public void setTopicId(UUID topicId) { this.topicId = topicId; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public BigDecimal getTopicScore() { return topicScore; }
    public void setTopicScore(BigDecimal topicScore) { this.topicScore = topicScore; }

    public Boolean getIsWeak() { return isWeak; }
    public void setIsWeak(Boolean isWeak) { this.isWeak = isWeak; }

    /** Composite PK class. */
    public static class SessionTopicId implements Serializable {
        private UUID sessionId;
        private UUID topicId;

        public SessionTopicId() {}
        public SessionTopicId(UUID sessionId, UUID topicId) {
            this.sessionId = sessionId;
            this.topicId = topicId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SessionTopicId that)) return false;
            return Objects.equals(sessionId, that.sessionId) && Objects.equals(topicId, that.topicId);
        }

        @Override
        public int hashCode() { return Objects.hash(sessionId, topicId); }
    }
}
