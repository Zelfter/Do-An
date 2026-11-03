package com.example.interview.infrastructure.http;

import com.example.interview.application.port.AiServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
}
