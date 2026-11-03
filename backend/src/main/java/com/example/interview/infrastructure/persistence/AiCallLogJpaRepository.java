package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AiCallLogJpaEntity} (table {@code ai_call_logs}).
 * NFR-24, AC-LOG-01: every call to /ai/v1/index/* produces a log entry.
 */
public interface AiCallLogJpaRepository extends JpaRepository<AiCallLogJpaEntity, UUID> {

    /** AC-LOG-01: count log entries by purpose (used in tests to verify logging). */
    long countByPurpose(String purpose);
}
