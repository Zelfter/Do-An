package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link StudyDocumentJpaEntity} (table {@code documents}).
 *
 * <p>Used by:
 * <ul>
 *   <li>{@link com.example.interview.application.topic.TopicService} — check linked docs (AC-TOPIC-02).</li>
 *   <li>{@link com.example.interview.application.document.StudyDocumentService} — CRUD + RAG pool.</li>
 * </ul>
 */
public interface StudyDocumentJpaRepository extends JpaRepository<StudyDocumentJpaEntity, UUID> {

    /** AC-TOPIC-02: count documents linked to a topic (DELETE blocked → 409). */
    long countByTopic_Id(UUID topicId);

    /** AC-TOPIC-02: check existence of linked documents. */
    boolean existsByTopic_Id(UUID topicId);

    /**
     * AC-DOC-01: only INDEXED documents appear in study materials.
     * Returns all documents for a given topic and language that have been indexed.
     */
    List<StudyDocumentJpaEntity> findByTopic_IdAndLanguageAndStatus(
            UUID topicId, String language, String status);

    /**
     * Returns all INDEXED documents regardless of topic — used for RAG pool retrieval.
     */
    List<StudyDocumentJpaEntity> findByStatus(String status);
}
