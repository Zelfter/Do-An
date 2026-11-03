package com.example.interview.application.topic;

import com.example.interview.api.admin.TopicRequest;
import com.example.interview.api.admin.TopicResponse;
import com.example.interview.api.common.ApiException;
import com.example.interview.infrastructure.persistence.QuestionJpaRepository;
import com.example.interview.infrastructure.persistence.StudyDocumentJpaRepository;
import com.example.interview.infrastructure.persistence.TopicAliasJpaEntity;
import com.example.interview.infrastructure.persistence.TopicAliasJpaRepository;
import com.example.interview.infrastructure.persistence.TopicJpaEntity;
import com.example.interview.infrastructure.persistence.TopicJpaRepository;
import com.example.interview.infrastructure.persistence.TopicTranslationJpaEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Application service for Admin topic management (Task 05, M2 — FR-37).
 *
 * <p>Encapsulates CRUD operations for topics, their translations and aliases.
 * Security rules enforced here:
 * <ul>
 *   <li>Code must be snake_case and unique → 409 on duplicate.</li>
 *   <li>At least one 'vi' translation is required on create (NFR-27).</li>
 *   <li>AC-TOPIC-02: DELETE blocked (409) when topic has linked questions or documents.</li>
 *   <li>AC-TOPIC-03: PATCH with {@code isActive=false} deactivates a topic.</li>
 *   <li>Code is immutable after creation ({@code updatable = false} in JPA entity).</li>
 * </ul>
 *
 * <p>Error messages are in Vietnamese (NFR-26).
 */
@Service
public class TopicService {

    static final String TOPIC_NOT_FOUND = "Không tìm thấy chủ đề";
    static final String CODE_DUPLICATE = "Chủ đề đã tồn tại với code này";
    static final String CODE_INVALID = "Code phải là snake_case (chữ thường, số, gạch dưới)";
    static final String TRANSLATION_VI_REQUIRED = "Phải có ít nhất một bản dịch tiếng Việt (vi)";
    static final String DELETE_BLOCKED = "Không thể xóa chủ đề khi có câu hỏi hoặc tài liệu liên kết";
    static final String ALIAS_DUPLICATE = "Alias đã tồn tại cho chủ đề này";
    static final String ALIAS_NOT_FOUND = "Không tìm thấy alias";
    static final String PARENT_NOT_FOUND = "Chủ đề cha không tồn tại";

    /** snake_case: starts with a lowercase letter, then lowercase/digits/underscores. */
    private static final Pattern SNAKE_CASE = Pattern.compile("^[a-z][a-z0-9_]*$");

    private final TopicJpaRepository topicRepository;
    private final QuestionJpaRepository questionRepository;
    private final StudyDocumentJpaRepository documentRepository;
    private final TopicAliasJpaRepository aliasRepository;

    public TopicService(TopicJpaRepository topicRepository,
                        QuestionJpaRepository questionRepository,
                        StudyDocumentJpaRepository documentRepository,
                        TopicAliasJpaRepository aliasRepository) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.documentRepository = documentRepository;
        this.aliasRepository = aliasRepository;
    }

    // ---- Create (POST) ---------------------------------------------------

    /**
     * AC-TOPIC-01: create a topic with translations and aliases.
     *
     * @throws ApiException 400 if code is not snake_case or 'vi' translation missing;
     *                       409 if code already exists.
     */
    @Transactional
    public TopicJpaEntity createTopic(TopicRequest.TopicCreateRequest request) {
        // Validate snake_case code.
        if (request.code() == null || !SNAKE_CASE.matcher(request.code()).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, CODE_INVALID);
        }

        // FR-37: code uniqueness → 409 on duplicate.
        if (topicRepository.existsByCodeIgnoreCase(request.code())) {
            throw new ApiException(HttpStatus.CONFLICT, CODE_DUPLICATE);
        }

        // NFR-27: at least one 'vi' translation required.
        boolean hasVi = request.translations() != null
                && request.translations().stream().anyMatch(t -> "vi".equals(t.language()));
        if (!hasVi) {
            throw new ApiException(HttpStatus.BAD_REQUEST, TRANSLATION_VI_REQUIRED);
        }

        TopicJpaEntity topic = new TopicJpaEntity();
        topic.setCode(request.code());

        // Optional parent for 1-level hierarchy (FR-37).
        if (request.parentId() != null) {
            TopicJpaEntity parent = findTopic(request.parentId());
            topic.setParent(parent);
        }

        // Add translations (cascade ALL).
        for (TopicResponse.TranslationDto t : request.translations()) {
            TopicTranslationJpaEntity translation = new TopicTranslationJpaEntity();
            translation.setTopic(topic);
            translation.setLanguage(t.language());
            translation.setName(t.name());
            translation.setDescription(t.description());
            topic.getTranslations().add(translation);
        }

        // Add aliases (optional).
        if (request.aliases() != null) {
            for (String alias : request.aliases()) {
                if (alias != null && !alias.isBlank()) {
                    TopicAliasJpaEntity aliasEntity = new TopicAliasJpaEntity();
                    aliasEntity.setTopic(topic);
                    aliasEntity.setAlias(alias);
                    topic.getAliases().add(aliasEntity);
                }
            }
        }

        TopicJpaEntity saved = topicRepository.save(topic);
        // Initialize lazy collections for DTO mapping outside the transaction.
        saved.getTranslations().size();
        saved.getAliases().size();
        return saved;
    }

    // ---- Read (GET) -------------------------------------------------------

    /** GET /api/v1/admin/topics/{id} → 404 when not found. */
    @Transactional(readOnly = true)
    public TopicJpaEntity getTopicById(UUID id) {
        TopicJpaEntity topic = findTopic(id);
        initLazyCollections(topic);
        return topic;
    }

    /** GET /api/v1/admin/topics → list all. */
    @Transactional(readOnly = true)
    public List<TopicJpaEntity> listTopics() {
        List<TopicJpaEntity> topics = topicRepository.findAll();
        topics.forEach(this::initLazyCollections);
        return topics;
    }

    // ---- Full update (PUT) -----------------------------------------------

    /**
     * PUT /api/v1/admin/topics/{id} — full update.
     * Code is immutable (not included in the request, JPA ignores updates to it).
     */
    @Transactional
    public TopicJpaEntity updateTopic(UUID id, TopicRequest.TopicUpdateRequest request) {
        TopicJpaEntity topic = findTopic(id);

        // Update parent.
        if (request.parentId() != null) {
            topic.setParent(findTopic(request.parentId()));
        } else {
            topic.setParent(null);
        }

        // Replace translations.
        if (request.translations() != null) {
            topic.getTranslations().clear();
            for (TopicResponse.TranslationDto t : request.translations()) {
                TopicTranslationJpaEntity translation = new TopicTranslationJpaEntity();
                translation.setTopic(topic);
                translation.setLanguage(t.language());
                translation.setName(t.name());
                translation.setDescription(t.description());
                topic.getTranslations().add(translation);
            }
        }

        // Replace aliases.
        if (request.aliases() != null) {
            topic.getAliases().clear();
            for (String alias : request.aliases()) {
                if (alias != null && !alias.isBlank()) {
                    TopicAliasJpaEntity aliasEntity = new TopicAliasJpaEntity();
                    aliasEntity.setTopic(topic);
                    aliasEntity.setAlias(alias);
                    topic.getAliases().add(aliasEntity);
                }
            }
        }

        TopicJpaEntity saved = topicRepository.save(topic);
        initLazyCollections(saved);
        return saved;
    }

    // ---- Partial update (PATCH) ------------------------------------------

    /**
     * PATCH /api/v1/admin/topics/{id} — partial update.
     * AC-TOPIC-03: {@code isActive = false} deactivates a topic.
     * Only non-null fields are applied.
     */
    @Transactional
    public TopicJpaEntity patchTopic(UUID id, TopicRequest.TopicPatchRequest request) {
        TopicJpaEntity topic = findTopic(id);

        if (request.parentId() != null) {
            topic.setParent(findTopic(request.parentId()));
        }

        if (request.isActive() != null) {
            topic.setActive(request.isActive());
        }

        if (request.translations() != null) {
            topic.getTranslations().clear();
            for (TopicResponse.TranslationDto t : request.translations()) {
                TopicTranslationJpaEntity translation = new TopicTranslationJpaEntity();
                translation.setTopic(topic);
                translation.setLanguage(t.language());
                translation.setName(t.name());
                translation.setDescription(t.description());
                topic.getTranslations().add(translation);
            }
        }

        if (request.aliases() != null) {
            topic.getAliases().clear();
            for (String alias : request.aliases()) {
                if (alias != null && !alias.isBlank()) {
                    TopicAliasJpaEntity aliasEntity = new TopicAliasJpaEntity();
                    aliasEntity.setTopic(topic);
                    aliasEntity.setAlias(alias);
                    topic.getAliases().add(aliasEntity);
                }
            }
        }

        TopicJpaEntity saved = topicRepository.save(topic);
        initLazyCollections(saved);
        return saved;
    }

    // ---- Delete (DELETE) --------------------------------------------------

    /**
     * AC-TOPIC-02: delete a topic.
     *
     * @throws ApiException 404 if topic not found;
     *                       409 if topic has linked questions or documents.
     */
    @Transactional
    public void deleteTopic(UUID id) {
        TopicJpaEntity topic = findTopic(id);

        long questionCount = questionRepository.countByTopic_Id(id);
        long documentCount = documentRepository.countByTopic_Id(id);

        if (questionCount > 0 || documentCount > 0) {
            throw new ApiException(HttpStatus.CONFLICT, DELETE_BLOCKED);
        }

        topicRepository.delete(topic);
    }

    // ---- Aliases ----------------------------------------------------------

    /** GET /api/v1/admin/topics/{id}/aliases → list aliases for a topic. */
    @Transactional(readOnly = true)
    public List<TopicAliasJpaEntity> listAliases(UUID topicId) {
        findTopic(topicId);
        return aliasRepository.findByTopic_Id(topicId);
    }

    /**
     * POST /api/v1/admin/topics/{id}/aliases → add an alias.
     *
     * @throws ApiException 404 if topic not found; 409 if alias already exists.
     */
    @Transactional
    public TopicAliasJpaEntity addAlias(UUID topicId, TopicRequest.AliasCreateRequest request) {
        TopicJpaEntity topic = findTopic(topicId);

        if (aliasRepository.existsByTopic_IdAndAliasIgnoreCase(topicId, request.alias())) {
            throw new ApiException(HttpStatus.CONFLICT, ALIAS_DUPLICATE);
        }

        TopicAliasJpaEntity alias = new TopicAliasJpaEntity();
        alias.setTopic(topic);
        alias.setAlias(request.alias());
        return aliasRepository.save(alias);
    }

    /**
     * DELETE /api/v1/admin/topics/{id}/aliases/{aliasId} → remove an alias.
     *
     * <p>Removes the alias from the owning side (topic's {@code aliases} collection)
     * so that {@code orphanRemoval = true} deletes it cleanly. Deleting directly
     * via the alias repository would leave the topic's collection inconsistent.
     *
     * @throws ApiException 404 if topic or alias not found (or alias doesn't belong to topic).
     */
    @Transactional
    public void deleteAlias(UUID topicId, UUID aliasId) {
        TopicJpaEntity topic = findTopic(topicId);

        TopicAliasJpaEntity alias = aliasRepository.findById(aliasId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ALIAS_NOT_FOUND));

        if (!alias.getTopic().getId().equals(topicId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, ALIAS_NOT_FOUND);
        }

        // Remove from the owning side — orphanRemoval deletes the row on flush.
        topic.getAliases().remove(alias);
    }

    // ---- Helpers ----------------------------------------------------------

    /** Finds a topic by id or throws 404. */
    private TopicJpaEntity findTopic(UUID id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, TOPIC_NOT_FOUND));
    }

    /** Initializes lazy collections so they can be accessed after the transaction ends. */
    private void initLazyCollections(TopicJpaEntity topic) {
        topic.getTranslations().size();
        topic.getAliases().size();
    }
}
