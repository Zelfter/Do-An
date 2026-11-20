package com.example.interview.api.candidate;

import com.example.interview.infrastructure.persistence.StudyDocumentJpaEntity;
import com.example.interview.infrastructure.persistence.TopicTranslationJpaEntity;

import java.time.Instant;
import java.util.UUID;

/** Read-only document projection returned to authenticated candidates and admins. */
public record CandidateDocumentDto(
        UUID id,
        UUID topicId,
        String topicCode,
        String topicName,
        String language,
        String title,
        String content,
        String contentMd,
        String fileUrl,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CandidateDocumentDto summaryFrom(StudyDocumentJpaEntity document) {
        return from(document, null);
    }

    public static CandidateDocumentDto detailFrom(StudyDocumentJpaEntity document) {
        return from(document, document.getContentMd());
    }

    private static CandidateDocumentDto from(StudyDocumentJpaEntity document, String content) {
        String topicCode = document.getTopic() != null ? document.getTopic().getCode() : null;
        String topicName = null;

        if (document.getTopic() != null) {
            topicName = document.getTopic().getTranslations().stream()
                    .filter(t -> "vi".equalsIgnoreCase(t.getLanguage()))
                    .map(TopicTranslationJpaEntity::getName)
                    .findFirst()
                    .orElseGet(() -> document.getTopic().getTranslations().stream()
                            .findFirst()
                            .map(TopicTranslationJpaEntity::getName)
                            .orElse(topicCode));
        }

        return new CandidateDocumentDto(
                document.getId(),
                document.getTopic() != null ? document.getTopic().getId() : null,
                topicCode,
                topicName,
                document.getLanguage(),
                document.getTitle(),
                content,
                content,
                null,
                document.getStatus(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
