package com.example.interview.application.question;

import com.example.interview.api.admin.QuestionRequest;
import com.example.interview.api.common.ApiException;
import com.example.interview.api.common.PublishValidationException;
import com.example.interview.application.document.AiCallLogService;
import com.example.interview.application.port.AiServiceClient;
import com.example.interview.infrastructure.persistence.QuestionJpaEntity;
import com.example.interview.infrastructure.persistence.QuestionJpaRepository;
import com.example.interview.infrastructure.persistence.QuestionKeyPointJpaEntity;
import com.example.interview.infrastructure.persistence.TopicJpaEntity;
import com.example.interview.infrastructure.persistence.TopicJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Application service for Admin question-bank management (Task 06, M2 — FR-32).
 *
 * <p>Encapsulates CRUD and lifecycle operations for questions. Key rules:
 * <ul>
 *   <li>Questions are created as {@code DRAFT} (FR-32).</li>
 *   <li><b>BR-19 / AC-Q-01:</b> {@code publish} validates that all mandatory
 *       fields are present (topic, level, content, non-blank reference_answer,
 *       ≥1 key_point). Missing fields → 422 with a {@code missingFields} list.</li>
 *   <li><b>AC-Q-03:</b> a successful publish sets {@code status=PUBLISHED} and
 *       triggers RAG indexing via {@link AiServiceClient}; {@code index_status}
 *       becomes {@code INDEXED} on success or {@code INDEX_FAILED} on failure.</li>
 *   <li><b>AC-Q-02 (≡ AC-19):</b> the RAG pool returns only
 *       {@code PUBLISHED + INDEXED} questions; DRAFT/ARCHIVED/INDEX_FAILED are
 *       excluded.</li>
 *   <li>Archiving sets {@code status=ARCHIVED} (soft removal from the pool).</li>
 * </ul>
 *
 * <p>Error messages are in Vietnamese (NFR-26).
 */
@Service
public class QuestionService {

    /** Allowed difficulty levels — BR-08. */
    private static final List<String> VALID_LEVELS = List.of("JUNIOR", "MID", "SENIOR");

    /** Allowed question statuses — FR-32. */
    static final String STATUS_DRAFT = "DRAFT";
    static final String STATUS_PUBLISHED = "PUBLISHED";
    static final String STATUS_ARCHIVED = "ARCHIVED";

    /** Indexing statuses — design §3.1. */
    static final String INDEX_NOT_INDEXED = "NOT_INDEXED";
    static final String INDEX_INDEXED = "INDEXED";
    static final String INDEX_FAILED = "INDEX_FAILED";

    static final String QUESTION_NOT_FOUND = "Không tìm thấy câu hỏi";
    static final String TOPIC_NOT_FOUND = "Chủ đề không tồn tại";
    static final String LEVEL_INVALID = "level phải là JUNIOR, MID hoặc SENIOR";
    static final String ALREADY_PUBLISHED = "Câu hỏi đã xuất bản";
    static final String ALREADY_ARCHIVED = "Câu hỏi đã lưu trữ";

    /** Purpose tag for AI call logs (AC-LOG-01, NFR-24). */
    private static final String LOG_PURPOSE = "INDEX_QUESTION";

    private final QuestionJpaRepository questionRepository;
    private final TopicJpaRepository topicRepository;
    private final AiServiceClient aiServiceClient;
    private final AiCallLogService aiCallLogService;

    public QuestionService(QuestionJpaRepository questionRepository,
                           TopicJpaRepository topicRepository,
                           AiServiceClient aiServiceClient,
                           AiCallLogService aiCallLogService) {
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.aiServiceClient = aiServiceClient;
        this.aiCallLogService = aiCallLogService;
    }

    // ---- Create (POST) ---------------------------------------------------

    /**
     * POST /api/v1/admin/questions — create a new DRAFT question.
     *
     * @throws ApiException 404 if topicId does not exist; 400 if level invalid.
     */
    @Transactional
    public QuestionJpaEntity createQuestion(QuestionRequest.QuestionCreateRequest request) {
        TopicJpaEntity topic = findTopic(request.topicId());
        validateLevel(request.level());

        QuestionJpaEntity question = new QuestionJpaEntity();
        question.setTopic(topic);
        question.setLanguage(request.language() != null && !request.language().isBlank()
                ? request.language() : "vi");
        question.setLevel(request.level());
        question.setContent(request.content());
        question.setReferenceAnswer(request.referenceAnswer() != null ? request.referenceAnswer() : "");
        question.setStatus(STATUS_DRAFT);
        question.setIndexStatus(INDEX_NOT_INDEXED);

        addKeyPoints(question, request.keyPoints());

        QuestionJpaEntity saved = questionRepository.save(question);
        initLazyKeyPoints(saved);
        return saved;
    }

    // ---- Read (GET) -------------------------------------------------------

    /** GET /api/v1/admin/questions/{id} → 404 when not found. */
    @Transactional(readOnly = true)
    public QuestionJpaEntity getQuestionById(UUID id) {
        QuestionJpaEntity question = findQuestion(id);
        initLazyKeyPoints(question);
        return question;
    }

    /** GET /api/v1/admin/questions → list all questions. */
    @Transactional(readOnly = true)
    public List<QuestionJpaEntity> listQuestions() {
        List<QuestionJpaEntity> questions = questionRepository.findAll();
        questions.forEach(this::initLazyKeyPoints);
        return questions;
    }

    // ---- Full update (PUT) -----------------------------------------------

    /** PUT /api/v1/admin/questions/{id} — full update. */
    @Transactional
    public QuestionJpaEntity updateQuestion(UUID id, QuestionRequest.QuestionUpdateRequest request) {
        QuestionJpaEntity question = findQuestion(id);
        validateLevel(request.level());

        question.setTopic(findTopic(request.topicId()));
        if (request.language() != null && !request.language().isBlank()) {
            question.setLanguage(request.language());
        }
        question.setLevel(request.level());
        question.setContent(request.content());
        question.setReferenceAnswer(request.referenceAnswer() != null ? request.referenceAnswer() : "");

        // Replace key points (orphanRemoval deletes the old rows on flush).
        question.getKeyPoints().clear();
        addKeyPoints(question, request.keyPoints());

        QuestionJpaEntity saved = questionRepository.save(question);
        initLazyKeyPoints(saved);
        return saved;
    }

    // ---- Delete (DELETE) --------------------------------------------------

    /**
     * DELETE /api/v1/admin/questions/{id}.
     *
     * <p>The 409 "used in session" guard will be wired in once interview sessions
     * are implemented (Task 09+); until then deletion is permitted.
     */
    @Transactional
    public void deleteQuestion(UUID id) {
        QuestionJpaEntity question = findQuestion(id);
        questionRepository.delete(question);
    }

    // ---- Publish (BR-19, AC-Q-01, AC-Q-03) --------------------------------

    /**
     * POST /api/v1/admin/questions/{id}/publish.
     *
     * <p>Validates all mandatory fields (BR-19). If any are missing →
     * {@link PublishValidationException} (mapped to 422). On success sets
     * {@code status=PUBLISHED} and triggers RAG indexing (AC-Q-03).
     *
     * @return the updated question entity (status=PUBLISHED, indexStatus=INDEXED on success)
     * @throws PublishValidationException 422 when required fields are missing
     * @throws ApiException 404 if question not found
     */
    @Transactional
    public QuestionJpaEntity publishQuestion(UUID id) {
        QuestionJpaEntity question = findQuestion(id);

        if (STATUS_PUBLISHED.equals(question.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, ALREADY_PUBLISHED);
        }

        List<String> missing = validatePublishFields(question);
        if (!missing.isEmpty()) {
            throw new PublishValidationException(missing);
        }

        question.setStatus(STATUS_PUBLISHED);

        // AC-Q-03: trigger indexing. The mock client always succeeds.
        boolean indexed = triggerIndexing(question);
        question.setIndexStatus(indexed ? INDEX_INDEXED : INDEX_FAILED);

        QuestionJpaEntity saved = questionRepository.save(question);
        initLazyKeyPoints(saved);
        return saved;
    }

    // ---- Archive ----------------------------------------------------------

    /**
     * POST /api/v1/admin/questions/{id}/archive.
     * Archive removes the question from the RAG pool (AC-Q-02).
     */
    @Transactional
    public QuestionJpaEntity archiveQuestion(UUID id) {
        QuestionJpaEntity question = findQuestion(id);

        if (STATUS_ARCHIVED.equals(question.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, ALREADY_ARCHIVED);
        }

        question.setStatus(STATUS_ARCHIVED);
        QuestionJpaEntity saved = questionRepository.save(question);
        initLazyKeyPoints(saved);
        return saved;
    }

    // ---- RAG pool (AC-Q-02 ≡ AC-19) --------------------------------------

    /**
     * Returns the RAG retrieval pool: only {@code PUBLISHED + INDEXED}
     * questions. DRAFT, ARCHIVED and INDEX_FAILED questions are excluded.
     */
    @Transactional(readOnly = true)
    public List<QuestionJpaEntity> getRagPool() {
        List<QuestionJpaEntity> pool = questionRepository
                .findByStatusAndIndexStatus(STATUS_PUBLISHED, INDEX_INDEXED);
        pool.forEach(this::initLazyKeyPoints);
        return pool;
    }

    /** Checks whether a specific question is in the RAG pool (AC-Q-02). */
    @Transactional(readOnly = true)
    public boolean isInRagPool(UUID id) {
        return questionRepository.existsByIdAndStatusAndIndexStatus(id, STATUS_PUBLISHED, INDEX_INDEXED);
    }

    // ---- Internal helpers -------------------------------------------------

    /** Validates BR-19 publish requirements, returning the list of missing field names. */
    private List<String> validatePublishFields(QuestionJpaEntity question) {
        List<String> missing = new ArrayList<>();

        if (question.getTopic() == null) {
            missing.add("topic");
        }
        if (question.getLevel() == null || question.getLevel().isBlank()) {
            missing.add("level");
        }
        if (question.getContent() == null || question.getContent().isBlank()) {
            missing.add("content");
        }
        if (question.getReferenceAnswer() == null || question.getReferenceAnswer().isBlank()) {
            missing.add("reference_answer");
        }
        if (question.getKeyPoints() == null || question.getKeyPoints().isEmpty()) {
            missing.add("key_points");
        }

        return missing;
    }

    /**
     * Calls the AI service to index the question for RAG (AC-Q-03, design §4.5).
     * Failures are swallowed (return {@code false}) so the admin is not
     * interrupted — the {@code index_status} records the outcome.
     * AC-LOG-01: always records an AiCallLog entry.
     */
    private boolean triggerIndexing(QuestionJpaEntity question) {
        String topicCode = question.getTopic() != null ? question.getTopic().getCode() : null;
        List<String> keyPointTexts = question.getKeyPoints().stream()
                .map(QuestionKeyPointJpaEntity::getContent)
                .toList();
        long start = System.currentTimeMillis();
        boolean success = false;
        String error = null;
        try {
            success = aiServiceClient.indexQuestion(
                    question.getId(),
                    topicCode,
                    question.getLanguage(),
                    question.getLevel(),
                    question.getContent(),
                    question.getReferenceAnswer(),
                    keyPointTexts
            );
        } catch (Exception ex) {
            error = ex.getMessage();
        }
        int latencyMs = (int) (System.currentTimeMillis() - start);

        // AC-LOG-01: log every /ai/v1/index/* call.
        aiCallLogService.record(
                LOG_PURPOSE,
                "mock",
                null,
                success ? "SUCCESS" : "FAILED",
                latencyMs,
                error
        );
        return success;
    }

    /** Adds key points from request DTOs, setting the owning back-reference. */
    private void addKeyPoints(QuestionJpaEntity question, List<QuestionRequest.KeyPointRequest> keyPoints) {
        if (keyPoints == null) {
            return;
        }
        for (QuestionRequest.KeyPointRequest kp : keyPoints) {
            QuestionKeyPointJpaEntity entity = new QuestionKeyPointJpaEntity();
            entity.setQuestion(question);
            entity.setSeq(kp.seq());
            entity.setContent(kp.content());
            question.getKeyPoints().add(entity);
        }
    }

    /** Validates that the level is one of the allowed enum values (BR-08). */
    private void validateLevel(String level) {
        if (level == null || !VALID_LEVELS.contains(level)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, LEVEL_INVALID);
        }
    }

    /** Finds a question by id or throws 404. */
    private QuestionJpaEntity findQuestion(UUID id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, QUESTION_NOT_FOUND));
    }

    /** Finds a topic by id or throws 404. */
    private TopicJpaEntity findTopic(UUID id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, TOPIC_NOT_FOUND));
    }

    /** Initializes the lazy key-points collection for safe DTO mapping. */
    private void initLazyKeyPoints(QuestionJpaEntity question) {
        question.getKeyPoints().size();
    }
}
