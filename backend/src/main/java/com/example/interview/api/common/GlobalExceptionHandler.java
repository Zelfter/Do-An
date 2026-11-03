package com.example.interview.api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;

/**
 * Translates exceptions into the uniform {@link ErrorResponse} JSON shape.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@link ApiException} — explicit status + message from the service layer.</li>
 *   <li>{@link MethodArgumentNotValidException} — Bean Validation failures → 400.</li>
 *   <li>Fallback {@link Exception} → 500 (message omitted in production).</li>
 * </ul>
 *
 * <p>Note: Spring Security 401/403 are handled inside the security filter chain
 * (see {@code SecurityConfig}'s entry point / access-denied handler), not here,
 * because they occur before a controller is reached.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.debug("API exception: status={} message={}", ex.getStatus().value(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getStatus().value(), ex.getMessage()));
    }

    /**
     * Publish validation failure → 422 with a {@code missingFields} array
     * (BR-19, AC-Q-01 ≡ AC-18).
     */
    @ExceptionHandler(PublishValidationException.class)
    public ResponseEntity<Object> handlePublishValidation(PublishValidationException ex) {
        log.debug("Publish validation: missingFields={}", ex.getMissingFields());
        var body = new com.example.interview.api.admin.QuestionResponse.PublishValidationError(
                ex.getMissingFields());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        String message = "Dữ liệu không hợp lệ";
        ErrorResponse body = new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), message, fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponse body = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Lỗi máy chủ nội bộ, vui lòng thử lại sau");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
