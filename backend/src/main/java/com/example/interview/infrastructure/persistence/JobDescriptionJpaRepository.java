package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for {@link JobDescriptionJpaEntity} (table {@code job_descriptions}). */
public interface JobDescriptionJpaRepository extends JpaRepository<JobDescriptionJpaEntity, UUID> {

    /** List a candidate's job descriptions, newest first. */
    List<JobDescriptionJpaEntity> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}
