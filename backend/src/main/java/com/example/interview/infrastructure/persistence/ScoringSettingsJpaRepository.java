package com.example.interview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link ScoringSettingsJpaEntity} (table {@code scoring_settings}). */
public interface ScoringSettingsJpaRepository extends JpaRepository<ScoringSettingsJpaEntity, String> {
}
