package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TopicJpaEntity} (table {@code topics}).
 *
 * <p>Used by {@link com.example.interview.application.topic.TopicService} for
 * topic CRUD operations (FR-37).
 */
public interface TopicJpaRepository extends JpaRepository<TopicJpaEntity, UUID> {

    /** FR-37: code uniqueness check for duplicate detection (409). */
    boolean existsByCodeIgnoreCase(String code);

    /** BR-07: stable lookup by immutable code. */
    Optional<TopicJpaEntity> findByCodeIgnoreCase(String code);
}
