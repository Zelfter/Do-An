package com.example.interview.application.port;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for communicating with the Python AI Service (design §3.5, §4.2).
 *
 * <p>Used by {@link com.example.interview.application.question.QuestionService}
 * and {@link com.example.interview.application.document.StudyDocumentService}
 * to index content into the RAG vector store (FR-32, FR-33).
 *
 * <p>The real HTTP implementation ({@code PythonAiServiceClient}) is a future task.
 * Until then {@link com.example.interview.infrastructure.http.MockAiServiceClient}
 * provides a no-op default so the backend compiles and indexing can be tested in isolation.
 */
public interface AiServiceClient {

    /**
     * Indexes (or re-indexes) a question into the RAG vector store.
     *
     * <p>Implementations must not throw on indexing failure; instead they
     * return {@code false} so the caller can record {@code index_status =
     * INDEX_FAILED} without surfacing an error to the Admin (AC-DOC-02,
     * design §4.5).
     *
     * @param questionId      UUID of the question being indexed
     * @param topicCode        stable topic code (BR-07) used as RAG filter
     * @param language         content language, default {@code "vi"} (NFR-26)
     * @param level            difficulty — {@code JUNIOR}/{@code MID}/{@code SENIOR}
     * @param content         the question text
     * @param referenceAnswer  the model answer used for scoring (BR-02)
     * @param keyPoints        ordered list of key points used as grounding context
     * @return {@code true} on success, {@code false} on failure
     */
    boolean indexQuestion(UUID questionId, String topicCode, String language, String level,
                          String content, String referenceAnswer, List<String> keyPoints);

    /**
     * Indexes (or re-indexes) a study document into the RAG vector store.
     *
     * <p>Implementations must not throw on indexing failure; instead they
     * return {@code false} so the caller can record {@code status =
     * INDEX_FAILED} without surfacing an error to the Admin (AC-DOC-02).
     *
     * @param documentId  UUID of the document being indexed
     * @param topicCode   stable topic code (BR-07)
     * @param language    content language, default {@code "vi"} (NFR-26)
     * @param title       document title
     * @param contentMd   Markdown body (max 100 KB — design §6)
     * @return {@code true} on success, {@code false} on failure
     */
    boolean indexDocument(UUID documentId, String topicCode, String language,
                          String title, String contentMd);

    /**
     * Analyses a raw job description (Python owns JD extraction — design §4).
     *
     * @return suggested level (JUNIOR/MID/SENIOR) and raw terms with importance;
     *         Java maps raw terms to known topics and makes the selection decision.
     */
    AiResults.JdAnalysisResult analyzeJd(String rawText);

    /**
     * Evaluates a candidate answer against the rubric (Python owns criterion
     * suggestions — design §4). Returns per-criterion scores (0–10); Java computes
     * the weighted total (BR-01) and decides follow-up eligibility (BR-05).
     */
    AiResults.TurnEvaluationResult evaluateTurn(String questionText, String referenceAnswer,
                                                List<String> keyPoints, String answerText, String level);

    /**
     * Generates the report prose summary (Python owns report prose — design §4).
     * On failure the caller falls back to a PARTIAL report (AC-22), preserving scores.
     */
    AiResults.ReportProse generateReport(AiResults.ReportContext context);
}
