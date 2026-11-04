package com.example.interview.infrastructure.persistence;

import com.example.interview.infrastructure.persistence.SessionTopicJpaEntity.SessionTopicId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for {@link SessionTopicJpaEntity} (table {@code session_topics}). */
public interface SessionTopicJpaRepository extends JpaRepository<SessionTopicJpaEntity, SessionTopicId> {

    /** BR-10/BR-14: all topic rows for a session. */
    List<SessionTopicJpaEntity> findBySessionId(UUID sessionId);
}
