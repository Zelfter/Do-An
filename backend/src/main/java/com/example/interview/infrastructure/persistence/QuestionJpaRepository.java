package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link QuestionJpaEntity} (table {@code questions}).
 *
 * <p>Used by {@link com.example.interview.application.topic.TopicService} to check
 * whether a topic has linked questions (AC-TOPIC-02: DELETE blocked → 409).
 */
public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, UUID> {

    /** AC-TOPIC-02: count questions linked to a topic. */
    long countByTopic_Id(UUID topicId);

    /** AC-TOPIC-02: check existence of linked questions. */
    boolean existsByTopic_Id(UUID topicId);

    /**
     * AC-Q-02 (≡ AC-19): the RAG retrieval pool is the set of published AND
     * successfully indexed questions. DRAFT, ARCHIVED and INDEX_FAILED
     * questions are excluded.
     */
    List<QuestionJpaEntity> findByStatusAndIndexStatus(String status, String indexStatus);

    /** Counts the RAG pool for a topic — used to check pool membership. */
    long countByStatusAndIndexStatus(String status, String indexStatus);

    /** Exists check for the RAG pool (used by AC-Q-02 assertions). */
    boolean existsByIdAndStatusAndIndexStatus(UUID id, String status, String indexStatus);
}
