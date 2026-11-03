package com.example.interview.api.common;

import org.springframework.http.HttpStatus;

/**
 * Application-level exception carrying an HTTP status and a clear message.
 *
 * <p>Mapped to a JSON {@link ErrorResponse} by {@link GlobalExceptionHandler}.
 * This keeps the auth/domain layer free of Spring MVC concerns while still
 * letting controllers signal precise HTTP codes (409 duplicate, 401 bad
 * credentials, 403 locked account, 401 invalid token).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
