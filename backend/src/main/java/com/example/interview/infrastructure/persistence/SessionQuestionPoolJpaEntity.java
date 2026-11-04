package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for session_question_pool (composite PK: session_id + question_id).
 *
 * <p>FR-15: the RAG question pool is computed ONCE at session creation and
 * persisted here, then used as the source of ORIGINAL questions (and the
 * fallback) during the chat. There is NO re-embedding during chat turns.
 */
@Entity
@Table(name = "session_question_pool")
@IdClass(SessionQuestionPoolJpaEntity.SessionQuestionPoolId.class)
public class SessionQuestionPoolJpaEntity {

    @Id
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Id
    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    /** Ordering of questions within the pool — lower rank asked first. */
    @Column(name = "rank", nullable = false)
    private int rank;

    /** True once the question has been asked (FR-22: no question repeats). */
    @Column(name = "used", nullable = false)
    private boolean used = false;

    // ---- Getters / Setters ----

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    /** Composite PK class. */
    public static class SessionQuestionPoolId implements Serializable {
        private UUID sessionId;
        private UUID questionId;

        public SessionQuestionPoolId() {}
        public SessionQuestionPoolId(UUID sessionId, UUID questionId) {
            this.sessionId = sessionId;
            this.questionId = questionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SessionQuestionPoolId that)) return false;
            return Objects.equals(sessionId, that.sessionId) && Objects.equals(questionId, that.questionId);
        }

        @Override
        public int hashCode() { return Objects.hash(sessionId, questionId); }
    }
}
