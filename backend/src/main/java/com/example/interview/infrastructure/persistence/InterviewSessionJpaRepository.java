package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for {@link InterviewSessionJpaEntity} (table {@code interview_sessions}). */
public interface InterviewSessionJpaRepository extends JpaRepository<InterviewSessionJpaEntity, UUID> {

    /** FR-24: list a candidate's sessions, newest first. */
    List<InterviewSessionJpaEntity> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}
