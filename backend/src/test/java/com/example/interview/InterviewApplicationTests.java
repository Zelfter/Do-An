package com.example.interview;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Task 01 — DoD: context loads without errors.
 * Uses H2 in-memory DB (application-test.yml) — NFR-33.
 * Flyway disabled in test profile to avoid pg-specific SQL on H2.
 */
@SpringBootTest
@ActiveProfiles("test")
class InterviewApplicationTests {

    @Test
    void contextLoads() {
        // If Spring context starts successfully, this test passes.
        // Verifies: all beans wire up, config properties are valid.
    }
}
