package com.example.interview.application.port;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result records for the interview-facing AI operations (the Java→Python contract).
 *
 * <p>These are intentionally plain records so both the mock client and a future
 * real {@code PythonAiServiceClient} (HTTP + JSON) can produce them. Per the
 * architecture guardrails, Python returns raw criterion values and prose;
 * Java owns the weighted total (BR-01), follow-up eligibility (BR-05) and all
 * business state.
 */
public final class AiResults {
    private AiResults() {}

    /** One raw term extracted from a JD, with suggested importance. */
    public record JdAnalysisTerm(String rawTerm, String importance) {}

    /** JD extraction result: suggested level + raw terms. Java maps terms to topics. */
    public record JdAnalysisResult(String level, List<JdAnalysisTerm> terms) {}

    /**
     * Per-criterion scores (0–10) from Python. Java computes the weighted total
     * (BR-01). {@code followUpQuestion} is the AI-proposed follow-up text (or null);
     * Java decides whether to actually ask it (BR-05).
     */
    public record TurnEvaluationResult(
            BigDecimal accuracy,
            BigDecimal completeness,
            BigDecimal clarity,
            BigDecimal exampleScore,
            List<String> missedKeyPoints,
            String followUpQuestion
    ) {}

    /** Context handed to the report-prose generator. */
    public record ReportContext(
            String level,
            int answered,
            int total,
            String totalScorePct,
            String classification,
            List<String> weakTopicNames
    ) {}

    /** Generated report prose; on AI failure Java falls back to PARTIAL (AC-22). */
    public record ReportProse(String summary) {}
}
