package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for {@link TurnEvaluationJpaEntity} (table {@code turn_evaluations}). */
public interface TurnEvaluationJpaRepository extends JpaRepository<TurnEvaluationJpaEntity, UUID> {

    /** BR-10/BR-11: all evaluations for a session (used to compute topic & total scores). */
    List<TurnEvaluationJpaEntity> findByTurn_Session_Id(UUID sessionId);
}
