package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data JPA repository for {@link SessionReportJpaEntity} (table {@code session_reports}). */
public interface SessionReportJpaRepository extends JpaRepository<SessionReportJpaEntity, UUID> {
}
