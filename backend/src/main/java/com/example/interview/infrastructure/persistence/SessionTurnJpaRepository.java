package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for {@link SessionTurnJpaEntity} (table {@code session_turns}). */
public interface SessionTurnJpaRepository extends JpaRepository<SessionTurnJpaEntity, UUID> {

    /** FR-23: ordered transcript for a session. */
    List<SessionTurnJpaEntity> findBySession_IdOrderByCreatedAtAsc(UUID sessionId);
}
