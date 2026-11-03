package com.example.interview.api.admin;

import com.example.interview.infrastructure.persistence.StudyDocumentJpaEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTOs for Admin document (study material) endpoints (design §3.4, FR-33).
 *
 * <ul>
 *   <li>{@link DocumentSummaryDto} — list projection (no contentMd).</li>
 *   <li>{@link DocumentDto} — full detail including contentMd and index info.</li>
 *   <li>{@link ReindexResult} — response of POST /{id}/reindex.</li>
 * </ul>
 */
public final class DocumentResponse {

    private DocumentResponse() {
    }

    /** Summary returned by GET /admin/documents list. */
    public record DocumentSummaryDto(
            UUID id,
            UUID topicId,
            String language,
            String title,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static DocumentSummaryDto from(StudyDocumentJpaEntity doc) {
            return new DocumentSummaryDto(
                    doc.getId(),
                    doc.getTopic() != null ? doc.getTopic().getId() : null,
                    doc.getLanguage(),
                    doc.getTitle(),
                    doc.getStatus(),
                    doc.getCreatedAt(),
                    doc.getUpdatedAt()
            );
        }
    }

    /** Full detail returned by GET /admin/documents/{id} and after create/update. */
    public record DocumentDto(
            UUID id,
            UUID topicId,
            String language,
            String title,
            String contentMd,
            String status,
            String indexError,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static DocumentDto from(StudyDocumentJpaEntity doc) {
            return new DocumentDto(
                    doc.getId(),
                    doc.getTopic() != null ? doc.getTopic().getId() : null,
                    doc.getLanguage(),
                    doc.getTitle(),
                    doc.getContentMd(),
                    doc.getStatus(),
                    doc.getIndexError(),
                    doc.getCreatedAt(),
                    doc.getUpdatedAt()
            );
        }
    }

    /** Response of POST /admin/documents/{id}/reindex. */
    public record ReindexResult(UUID documentId, String status, String indexError) {
        public static ReindexResult from(StudyDocumentJpaEntity doc) {
            return new ReindexResult(doc.getId(), doc.getStatus(), doc.getIndexError());
        }
    }
}
