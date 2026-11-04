package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for {@link JdTopicJpaEntity} (table {@code jd_topics}). */
public interface JdTopicJpaRepository extends JpaRepository<JdTopicJpaEntity, UUID> {

    /** FR-06/FR-07: all topic mappings for a JD. */
    List<JdTopicJpaEntity> findByJobDescription_Id(UUID jdId);
}
