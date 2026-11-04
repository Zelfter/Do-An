package com.example.interview.api.interview;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

/**
 * Request DTOs for the candidate interview endpoints.
 *
 * <p>Validation uses Jakarta Bean Validation, mirroring the admin request DTOs.
 * Vietnamese error messages are produced by the service layer / GlobalExceptionHandler.
 */
public final class InterviewRequest {
    private InterviewRequest() {}

    /** POST /api/v1/jd/analyze — candidate pastes a job description (FR-04/FR-06). */
    public record AnalyzeJdRequest(@NotBlank(message = "Nội dung JD không được để trống") String rawText) {}

    /** POST /api/v1/sessions — create a session from an analysed JD (FR-09/FR-15). */
    public record CreateSessionRequest(
            UUID jdId,
            String level,
            Integer timeLimitSec,
            List<UUID> selectedTopicIds
    ) {}

    /** POST /api/v1/sessions/{id}/turns/{turnId}/answer — idempotent answer submit (BR-18). */
    public record AnswerRequest(
            @NotBlank(message = "requestId là bắt buộc") String requestId,
            @NotBlank(message = "Câu trả lời không được để trống") String answerText
    ) {}

    /** POST /api/v1/sessions/{id}/turns/{turnId}/skip — skip the current question. */
    public record SkipRequest(@NotBlank(message = "requestId là bắt buộc") String requestId) {}
}
