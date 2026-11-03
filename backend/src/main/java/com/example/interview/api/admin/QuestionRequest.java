package com.example.interview.api.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTOs for the Admin question endpoints (design §3.3, FR-32).
 *
 * <p>Mirrors the structure of {@link TopicRequest}: a private constructor and
 * public records nested inside.
 *
 * <ul>
 *   <li>{@link QuestionCreateRequest} — POST /admin/questions.</li>
 *   <li>{@link QuestionUpdateRequest} — PUT /admin/questions/{id}.</li>
 *   <li>{@link KeyPointRequest} — ordered key point (BR-02).</li>
 * </ul>
 *
 * <p>Bean Validation enforces structural invariants only (non-null topic,
 * non-blank level/content). The richer publish validation (reference_answer
 * non-empty and ≥1 key point) is enforced in {@code QuestionService} because
 * those fields are optional on create but mandatory on publish (BR-19).
 */
public final class QuestionRequest {

    private QuestionRequest() {
    }

    /**
     * POST /api/v1/admin/questions — create a DRAFT question.
     *
     * @param topicId         FK to topics; required (404 if missing).
     * @param language        default {@code "vi"} when null (NFR-26).
     * @param level           JUNIOR | MID | SENIOR (BR-08).
     * @param content         question text.
     * @param referenceAnswer optional on create, required on publish (BR-19).
     * @param keyPoints       optional on create, ≥1 required on publish (BR-19).
     */
    public record QuestionCreateRequest(
            @NotNull(message = "topicId không được để trống")
            UUID topicId,
            String language,
            @NotBlank(message = "level không được để trống")
            String level,
            @NotBlank(message = "content không được để trống")
            String content,
            String referenceAnswer,
            @Valid List<KeyPointRequest> keyPoints
    ) {
    }

    /** PUT /api/v1/admin/questions/{id} — full update. */
    public record QuestionUpdateRequest(
            @NotNull(message = "topicId không được để trống")
            UUID topicId,
            String language,
            @NotBlank(message = "level không được để trống")
            String level,
            @NotBlank(message = "content không được để trống")
            String content,
            String referenceAnswer,
            @Valid List<KeyPointRequest> keyPoints
    ) {
    }

    /** Ordered key point — {@code seq} starts at 1 (BR-02). */
    public record KeyPointRequest(
            int seq,
            @NotBlank(message = "content key point không được để trống")
            String content
    ) {
    }
}
