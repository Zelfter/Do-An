package com.example.interview.application.document;

import com.example.interview.infrastructure.persistence.AiCallLogJpaEntity;
import com.example.interview.infrastructure.persistence.AiCallLogJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records every Java→Python AI Service call in {@code ai_call_logs}.
 * NFR-24, AC-LOG-01: purpose, provider, latency_ms, status are mandatory.
 *
 * <p>Uses {@code REQUIRES_NEW} propagation so the log entry is always
 * committed even if the outer transaction rolls back.
 */
@Service
public class AiCallLogService {

    private final AiCallLogJpaRepository logRepository;

    public AiCallLogService(AiCallLogJpaRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * Persists an AI call log entry.
     *
     * @param purpose    e.g. {@code "INDEX_QUESTION"} or {@code "INDEX_DOCUMENT"}
     * @param provider   provider name from the client (e.g. {@code "mock"})
     * @param model      model identifier (may be null for mock)
     * @param status     {@code "SUCCESS"}, {@code "FAILED"}, or {@code "TIMEOUT"}
     * @param latencyMs  wall-clock milliseconds from Java perspective
     * @param error      error detail when status != SUCCESS; null otherwise
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String purpose, String provider, String model,
                       String status, int latencyMs, String error) {
        AiCallLogJpaEntity entry = new AiCallLogJpaEntity();
        entry.setPurpose(purpose);
        entry.setProvider(provider);
        entry.setModel(model);
        entry.setStatus(status);
        entry.setLatencyMs(latencyMs);
        entry.setError(error);
        logRepository.save(entry);
    }
}
