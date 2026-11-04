package com.example.interview.api.interview;

import java.util.List;

/**
 * Response DTOs for the candidate interview endpoints.
 *
 * <p>These mirror the TypeScript contract in {@code frontend/src/api/interviewApi.ts}.
 *
 * <p>BR-20 / AC-05 / AC-12: {@link SessionStateDto} and {@link TurnTranscriptDto} carry
 * NO score, criterion or feedback fields while a session is running. Scores appear
 * only in {@link SessionReportDto} / {@link TurnScoreDto}, served once the session
 * has ended.
 */
public final class InterviewResponse {
    private InterviewResponse() {}

    // ---- Live session state (score-free while IN_PROGRESS — BR-20) ------------

    public record SessionTopicDto(String topicId, String code, String name) {}

    public record TurnTranscriptDto(
            String turnId,
            String kind,
            String questionText,
            String answerText,   // null while the turn is still PENDING
            String topicName,
            String topicCode
    ) {}

    public record CurrentTurnDto(
            String turnId,
            String kind,
            String questionText,
            String topicName,
            String topicCode
    ) {}

    public record SessionStateDto(
            String sessionId,
            String status,
            String level,
            int timeLimitSec,
            long elapsedMs,
            int remainingSec,
            List<SessionTopicDto> topics,
            int totalQuestions,
            int answeredCount,
            int followUpCount,
            List<TurnTranscriptDto> turns,
            CurrentTurnDto currentTurn,
            String endedAt
    ) {}

    public record SessionSummaryDto(
            String sessionId,
            String status,
            String level,
            Double totalScore,   // null until the session has ended
            String createdAt
    ) {}

    public record EndSessionDto(String sessionId, String status, String endedAt) {}

    // ---- JD analysis (FR-06) --------------------------------------------------

    public record JdTopicDto(
            String jdTopicId,
            String rawTerm,
            String topicId,    // null for unsupported terms (FR-38)
            String topicCode,
            String importance,
            boolean selected
    ) {}

    public record JdAnalysisDto(String jdId, String level, List<JdTopicDto> terms) {}

    // ---- Report (scores only available after the session ends — BR-20) --------

    public record TopicScoreDto(
            String topicId,
            String code,
            String name,
            Double topicScore,   // null if no evaluable turns for the topic
            boolean isWeak
    ) {}

    public record TurnScoreDto(
            String turnId,
            String kind,
            String questionText,
            String answerText,
            String status,
            Double accuracy,
            Double completeness,
            Double clarity,
            Double exampleScore,
            Double totalScore,   // null for UNGRADED turns
            List<String> missedKeyPoints,
            String feedback
    ) {}

    public record StudyMaterialDto(String topicId, String topicName, String documentId, String title) {}

    public record SessionReportDto(
            String sessionId,
            String status,
            String reportStatus,
            String level,
            double totalScore,
            String classification,
            List<TopicScoreDto> topics,
            List<TopicScoreDto> weakTopics,
            List<TurnScoreDto> turns,
            String summary,
            List<StudyMaterialDto> studyMaterials,
            String generatedAt
    ) {}
}
