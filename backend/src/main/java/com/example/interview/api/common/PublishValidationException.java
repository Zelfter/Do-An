package com.example.interview.api.common;

import java.util.List;

/**
 * Thrown by {@link com.example.interview.application.question.QuestionService}
 * when a question cannot be published because one or more mandatory fields
 * are missing (BR-19, AC-Q-01 ≡ AC-18).
 *
 * <p>Carries the list of missing field names so that
 * {@link GlobalExceptionHandler} can render a 422 response with a
 * {@code missingFields} array, e.g.
 * {@code {"status":422,"message":"Thiếu trường bắt buộc",
 *         "missingFields":["reference_answer","key_points"]}}.
 */
public class PublishValidationException extends RuntimeException {

    private final List<String> missingFields;

    public PublishValidationException(List<String> missingFields) {
        super("Thiếu trường bắt buộc");
        this.missingFields = List.copyOf(missingFields);
    }

    public List<String> getMissingFields() {
        return missingFields;
    }
}
