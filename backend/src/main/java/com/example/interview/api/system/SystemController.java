package com.example.interview.api.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Warm-up endpoint — UC-18, NFR-31, AC-23.
 *
 * GET /api/v1/system/warmup — permitAll (no JWT required).
 * Frontend calls this on page load to wake Java backend and trigger AI service warm-up.
 * Timer never counts warm-up time against the candidate (NFR-31).
 *
 * In Task 07 this will also call Python AI Service /ai/v1/health in parallel.
 * For Task 01, returns basic status only.
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);

    /**
     * Returns backend status.
     * AI service status will be added in Task 07 (indexing orchestration).
     */
    @GetMapping("/warmup")
    public ResponseEntity<Map<String, Object>> warmup() {
        log.info("Warm-up requested at {}", Instant.now());
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "backend",
                "timestamp", Instant.now().toString()
        ));
    }
}
