package com.example.interview.infrastructure.persistence;

import com.example.interview.infrastructure.persistence.SessionQuestionPoolJpaEntity.SessionQuestionPoolId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for {@link SessionQuestionPoolJpaEntity} (table {@code session_question_pool}). */
public interface SessionQuestionPoolJpaRepository
        extends JpaRepository<SessionQuestionPoolJpaEntity, SessionQuestionPoolId> {

    /** FR-15: full persisted pool for a session, ordered by rank. */
    List<SessionQuestionPoolJpaEntity> findBySessionIdOrderByRankAsc(UUID sessionId);

    /** FR-22: the next unused question to ask. */
    List<SessionQuestionPoolJpaEntity> findBySessionIdAndUsedFalseOrderByRankAsc(UUID sessionId);
}
