package com.example.interview.infrastructure.http;

import com.example.interview.application.port.AiResults;
import com.example.interview.application.port.AiServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Default no-op implementation of {@link AiServiceClient} used until the real
 * Python AI Service client ({@code PythonAiServiceClient}) is wired in.
 *
 * <p>Registered as a fallback bean by {@link AiServiceClientConfig} via
 * {@code @ConditionalOnMissingBean}, so it is automatically superseded once the
 * HTTP client implementation is added. It always reports success so that a
 * publish/create flow in tests/local development completes and index_status
 * becomes {@code INDEXED} (AC-Q-03, AC-DOC-01).
 *
 * <p>This is a plain class (not a {@code @Component}) — it is instantiated by
 * the configuration class to avoid the {@code @ConditionalOnMissingBean}
 * self-reference problem that occurs when a conditional annotation is placed
 * directly on a bean implementing the conditional type.
 */
public class MockAiServiceClient implements AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MockAiServiceClient.class);

    @Override
    public boolean indexQuestion(UUID questionId, String topicCode, String language, String level,
                                  String content, String referenceAnswer, List<String> keyPoints) {
        log.info("[MOCK] indexQuestion: questionId={} topicCode={} language={} level={} keyPoints={}",
                questionId, topicCode, language, level, keyPoints == null ? 0 : keyPoints.size());
        return true;
    }

    @Override
    public boolean indexDocument(UUID documentId, String topicCode, String language,
                                  String title, String contentMd) {
        log.info("[MOCK] indexDocument: documentId={} topicCode={} language={} title={}",
                documentId, topicCode, language, title);
        return true;
    }

    // ---- Interview operations (deterministic, no network, no paid LLM) --------

    @Override
    public AiResults.JdAnalysisResult analyzeJd(String rawText) {
        log.info("[MOCK] analyzeJd: length={}", rawText == null ? 0 : rawText.length());
        String lower = rawText == null ? "" : rawText.toLowerCase(Locale.ROOT);

        String level = "MID";
        if (lower.contains("senior") || lower.contains("lead") || lower.contains("principal")) {
            level = "SENIOR";
        } else if (lower.contains("junior") || lower.contains("fresher") || lower.contains("intern")) {
            level = "JUNIOR";
        }

        List<AiResults.JdAnalysisTerm> terms = new ArrayList<>();
        String[][] known = {
                {"spring boot", "Spring Boot"}, {"spring", "Spring"}, {"jpa", "JPA"}, {"hibernate", "Hibernate"},
                {"microservice", "Microservice"}, {"rest", "REST"}, {"api", "API"},
                {"multithreading", "Multi-threading"}, {"concurrency", "Concurrency"}, {"thread", "Concurrency"},
                {"collections", "Collections"}, {"stream", "Java 8 Stream"}, {"lambda", "Java 8 Lambda"},
                {"oop", "OOP"}, {"object-oriented", "OOP"},
                {"jvm", "JVM"}, {"garbage", "Garbage Collection"}, {"memory", "JVM Memory"},
                {"sql", "SQL"}, {"database", "Database"}, {"kafka", "Kafka"}, {"docker", "Docker"},
        };
        for (String[] k : known) {
            if (lower.contains(k[0])) {
                terms.add(new AiResults.JdAnalysisTerm(k[1], "PREFERRED"));
            }
        }
        if (terms.isEmpty()) {
            terms.add(new AiResults.JdAnalysisTerm("Java", "REQUIRED"));
        }
        return new AiResults.JdAnalysisResult(level, terms);
    }

    @Override
    public AiResults.TurnEvaluationResult evaluateTurn(String questionText, String referenceAnswer,
                                                       List<String> keyPoints, String answerText, String level) {
        log.info("[MOCK] evaluateTurn: answerLength={}", answerText == null ? 0 : answerText.length());
        String ans = answerText == null ? "" : answerText.toLowerCase(Locale.ROOT);
        String ref = referenceAnswer == null ? "" : referenceAnswer.toLowerCase(Locale.ROOT);

        // Completeness: fraction of key points whose tokens appear in the answer.
        int covered = 0;
        List<String> missed = new ArrayList<>();
        if (keyPoints != null && !keyPoints.isEmpty()) {
            for (String kp : keyPoints) {
                if (tokenMatch(ans, kp)) {
                    covered++;
                } else {
                    missed.add(kp);
                }
            }
        }
        BigDecimal completeness = (keyPoints == null || keyPoints.isEmpty())
                ? bd(7)
                : bd(Math.min(10, covered * 10.0 / keyPoints.size()));

        // Accuracy: token overlap with the reference answer.
        BigDecimal accuracy = bd(overlapScore(ans, ref));

        // Clarity: length-based proxy (longer structured answer = clearer), capped at 10.
        int len = answerText == null ? 0 : answerText.trim().length();
        BigDecimal clarity = bd(Math.min(10, len / 30.0));

        // Example: rewards concrete examples.
        boolean hasExample = ans.contains("ví dụ") || ans.contains("example") || ans.contains("e.g")
                || ans.contains("chẳng hạn") || ans.contains("như ");
        BigDecimal exampleScore = bd(hasExample ? 8 : (len >= 120 ? 6 : 3));

        // Follow-up proposal only when the answer looks below "good".
        String followUp = null;
        double approxPct = (accuracy.doubleValue() + completeness.doubleValue()
                + clarity.doubleValue() + exampleScore.doubleValue()) / 4.0 * 10.0;
        if (approxPct < 70 && keyPoints != null && !keyPoints.isEmpty()) {
            String firstMissed = missed.isEmpty() ? keyPoints.get(0) : missed.get(0);
            followUp = "Bạn có thể nói rõ hơn và cho ví dụ về \"" + firstMissed + "\" được không?";
        }
        return new AiResults.TurnEvaluationResult(accuracy, completeness, clarity, exampleScore, missed, followUp);
    }

    @Override
    public AiResults.ReportProse generateReport(AiResults.ReportContext ctx) {
        log.info("[MOCK] generateReport: answered={}/{} pct={}", ctx.answered(), ctx.total(), ctx.totalScorePct());
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn đã hoàn thành ").append(ctx.answered()).append("/").append(ctx.total())
                .append(" câu hỏi ở cấp độ ").append(ctx.level()).append(". ");
        sb.append("Tổng điểm: ").append(ctx.totalScorePct()).append("% — xếp loại: ")
                .append(ctx.classification()).append(". ");
        if (ctx.weakTopicNames() == null || ctx.weakTopicNames().isEmpty()) {
            sb.append("Bạn thể hiện khá đồng đều ở các chủ đề. Hãy tiếp tục luyện tập để duy trì mức độ.");
        } else {
            sb.append("Các chủ đề cần củng cố: ").append(String.join(", ", ctx.weakTopicNames()))
                    .append(". Hãy ôn lại tài liệu tham khảo cho những chủ đề này và làm lại phỏng vấn để cải thiện.");
        }
        return new AiResults.ReportProse(sb.toString());
    }

    // ---- helpers ----

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean tokenMatch(String answer, String keyPoint) {
        for (String t : keyPoint.toLowerCase(Locale.ROOT).split("\\W+")) {
            if (t.length() >= 4 && answer.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private static double overlapScore(String answer, String reference) {
        Set<String> seen = new HashSet<>();
        int total = 0, hit = 0;
        for (String t : reference.split("\\W+")) {
            if (t.length() < 4 || !seen.add(t)) {
                continue;
            }
            total++;
            if (answer.contains(t)) {
                hit++;
            }
        }
        if (total == 0) {
            return 5.0;
        }
        return Math.min(10, hit * 10.0 / total);
    }
}
