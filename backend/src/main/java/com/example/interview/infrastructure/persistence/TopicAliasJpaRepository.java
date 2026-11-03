package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TopicAliasJpaEntity} (table {@code topic_aliases}).
 *
 * <p>Used by {@link com.example.interview.application.topic.TopicService} for
 * alias CRUD operations (FR-37).
 */
public interface TopicAliasJpaRepository extends JpaRepository<TopicAliasJpaEntity, UUID> {

    /** FR-37: list all aliases for a topic. */
    List<TopicAliasJpaEntity> findByTopic_Id(UUID topicId);

    /** FR-37: duplicate alias check (case-insensitive). */
    boolean existsByTopic_IdAndAliasIgnoreCase(UUID topicId, String alias);
}
