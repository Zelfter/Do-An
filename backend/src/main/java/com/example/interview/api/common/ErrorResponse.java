package com.example.interview.api.common;

import java.time.Instant;
import java.util.List;

/**
 * Uniform JSON error body returned by the API.
 *
 * <p>{@code message} is always present (AC-AUTH-02: "message rõ ràng"). Field-level
 * validation errors are surfaced via {@link #errors} when applicable.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String message,
        List<String> errors
) {
    public ErrorResponse(int status, String message) {
        this(Instant.now(), status, message, List.of());
    }
}
