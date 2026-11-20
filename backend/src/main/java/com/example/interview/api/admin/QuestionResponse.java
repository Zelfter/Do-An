package com.example.interview.api.admin;

import com.example.interview.infrastructure.persistence.QuestionJpaEntity;
import com.example.interview.infrastructure.persistence.QuestionKeyPointJpaEntity;

import java.util.List;
import java.util.UUID;

/**
 * Response DTOs for the Admin question endpoints (design §3.3, FR-32).
 *
 * <p>Mirrors the structure of {@link TopicResponse}: a private constructor and
 * public records nested inside.
 *
 * <ul>
 *   <li>{@link QuestionSummaryDto} — list projection (no key points).</li>
 *   <li>{@link QuestionDetailDto} — full detail including key points.</li>
 *   <li>{@link KeyPointDto} — single key point projection.</li>
 *   <li>{@link PublishResult} — response of {@code POST /{id}/publish}.</li>
 *   <li>{@link PublishValidationError} — 422 body listing missing fields (AC-Q-01).</li>
 * </ul>
 */
public final class QuestionResponse {

    private QuestionResponse() {
    }

    /** Summary projection returned by {@code GET /admin/questions}. */
    public record QuestionSummaryDto(
            UUID id,
            UUID topicId,
            String language,
            String level,
            String content,
            String status,
            String indexStatus
    ) {
        public static QuestionSummaryDto from(QuestionJpaEntity q) {
            return new QuestionSummaryDto(
                    q.getId(),
                    q.getTopic() != null ? q.getTopic().getId() : null,
                    q.getLanguage(),
                    q.getLevel(),
                    q.getContent(),
                    q.getStatus(),
                    q.getIndexStatus()
            );
        }
    }

    /** Full detail returned by {@code GET /admin/questions/{id}}. */
    public record QuestionDetailDto(
            UUID id,
            UUID topicId,
            String language,
            String level,
            String content,
            String referenceAnswer,
            String followUpQuestion,
            String status,
            String indexStatus,
            List<KeyPointDto> keyPoints
    ) {
        /** Projects a JPA entity (with initialized lazy key points) into the DTO. */
        public static QuestionDetailDto from(QuestionJpaEntity q) {
            List<KeyPointDto> keyPoints = q.getKeyPoints().stream()
                    .map(KeyPointDto::from)
                    .toList();
            return new QuestionDetailDto(
                    q.getId(),
                    q.getTopic() != null ? q.getTopic().getId() : null,
                    q.getLanguage(),
                    q.getLevel(),
                    q.getContent(),
                    q.getReferenceAnswer(),
                    q.getFollowUpQuestion(),
                    q.getStatus(),
                    q.getIndexStatus(),
                    keyPoints
            );
        }
    }

    /** Single key point projection — preserves {@code seq} order (BR-02). */
    public record KeyPointDto(UUID id, int seq, String content) {
        public static KeyPointDto from(QuestionKeyPointJpaEntity kp) {
            return new KeyPointDto(kp.getId(), kp.getSeq(), kp.getContent());
        }
    }

    /** Response of {@code POST /admin/questions/{id}/publish} (AC-Q-03). */
    public record PublishResult(UUID id, String status, String indexStatus, boolean indexingTriggered) {
        public static PublishResult from(QuestionJpaEntity q, boolean indexingTriggered) {
            return new PublishResult(q.getId(), q.getStatus(), q.getIndexStatus(), indexingTriggered);
        }
    }

    /**
     * 422 body for publish validation failure (AC-Q-01 ≡ AC-18).
     * <pre>{"status":422,"message":"Thiếu trường bắt buộc","missingFields":["reference_answer","key_points"]}</pre>
     */
    public record PublishValidationError(
            int status,
            String message,
            List<String> missingFields
    ) {
        public PublishValidationError(List<String> missingFields) {
            this(422, "Thiếu trường bắt buộc", missingFields);
        }
    }
}
