package com.example.interview.application.document;

import com.example.interview.api.admin.DocumentRequest;
import com.example.interview.api.common.ApiException;
import com.example.interview.application.port.AiServiceClient;
import com.example.interview.infrastructure.persistence.StudyDocumentJpaEntity;
import com.example.interview.infrastructure.persistence.StudyDocumentJpaRepository;
import com.example.interview.infrastructure.persistence.TopicJpaEntity;
import com.example.interview.infrastructure.persistence.TopicJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for Admin study document management (Task 07, M2+M3 — FR-33).
 *
 * <p>Key rules:
 * <ul>
 *   <li>Documents are created as {@code DRAFT}.</li>
 *   <li>Auto-trigger indexing on create and on explicit reindex request (FR-33).</li>
 *   <li><b>AC-DOC-01:</b> only {@code INDEXED} documents appear in study materials.</li>
 *   <li><b>AC-DOC-02:</b> indexing failure → status={@code INDEX_FAILED}; Java never
 *       throws to the Admin; HTTP 201 is still returned.</li>
 *   <li><b>AC-LOG-01:</b> every AI call is logged via {@link AiCallLogService}.</li>
 * </ul>
 *
 * <p>Error messages are in Vietnamese (NFR-26).
 */
@Service
public class StudyDocumentService {

    static final String STATUS_DRAFT = "DRAFT";
    static final String STATUS_INDEXED = "INDEXED";
    static final String STATUS_INDEX_FAILED = "INDEX_FAILED";

    static final String DOCUMENT_NOT_FOUND = "Không tìm thấy tài liệu";
    static final String TOPIC_NOT_FOUND = "Chủ đề không tồn tại";

    /** Purpose tag for AI call logs (AC-LOG-01, NFR-24). */
    private static final String LOG_PURPOSE = "INDEX_DOCUMENT";

    private final StudyDocumentJpaRepository documentRepository;
    private final TopicJpaRepository topicRepository;
    private final AiServiceClient aiServiceClient;
    private final AiCallLogService aiCallLogService;

    public StudyDocumentService(StudyDocumentJpaRepository documentRepository,
                                 TopicJpaRepository topicRepository,
                                 AiServiceClient aiServiceClient,
                                 AiCallLogService aiCallLogService) {
        this.documentRepository = documentRepository;
        this.topicRepository = topicRepository;
        this.aiServiceClient = aiServiceClient;
        this.aiCallLogService = aiCallLogService;
    }

    // ---- Create (POST) ---------------------------------------------------

    /**
     * POST /api/v1/admin/documents — create document, auto-trigger indexing.
     *
     * <p>Saves with DRAFT first, then calls indexing. On success → INDEXED;
     * on failure → INDEX_FAILED. Either way, HTTP 201 is returned (AC-DOC-02).
     */
    @Transactional
    public StudyDocumentJpaEntity createDocument(DocumentRequest.DocumentCreateRequest request) {
        TopicJpaEntity topic = findTopic(request.topicId());

        StudyDocumentJpaEntity doc = new StudyDocumentJpaEntity();
        doc.setTopic(topic);
        doc.setLanguage(request.language() != null && !request.language().isBlank()
                ? request.language() : "vi");
        doc.setTitle(request.title());
        doc.setContentMd(request.contentMd());
        doc.setStatus(STATUS_DRAFT);

        StudyDocumentJpaEntity saved = documentRepository.save(doc);

        // Auto-trigger indexing (FR-33); never throw to Admin (AC-DOC-02).
        triggerIndexing(saved);

        return documentRepository.save(saved);
    }

    // ---- Read (GET) -------------------------------------------------------

    /** GET /api/v1/admin/documents/{id} → 404 when not found. */
    @Transactional(readOnly = true)
    public StudyDocumentJpaEntity getDocumentById(UUID id) {
        return findDocument(id);
    }

    /** GET /api/v1/admin/documents → list all documents. */
    @Transactional(readOnly = true)
    public List<StudyDocumentJpaEntity> listDocuments() {
        return documentRepository.findAll();
    }

    // ---- Full update (PUT) -----------------------------------------------

    /** PUT /api/v1/admin/documents/{id} — full update (does not auto-reindex). */
    @Transactional
    public StudyDocumentJpaEntity updateDocument(UUID id,
                                                  DocumentRequest.DocumentUpdateRequest request) {
        StudyDocumentJpaEntity doc = findDocument(id);

        if (request.topicId() != null) {
            doc.setTopic(findTopic(request.topicId()));
        }
        if (request.language() != null && !request.language().isBlank()) {
            doc.setLanguage(request.language());
        }
        if (request.title() != null) {
            doc.setTitle(request.title());
        }
        if (request.contentMd() != null) {
            doc.setContentMd(request.contentMd());
        }

        return documentRepository.save(doc);
    }

    // ---- Delete (DELETE) -------------------------------------------------

    /** DELETE /api/v1/admin/documents/{id} → 204 | 404. */
    @Transactional
    public void deleteDocument(UUID id) {
        StudyDocumentJpaEntity doc = findDocument(id);
        documentRepository.delete(doc);
    }

    // ---- Reindex (POST) --------------------------------------------------

    /**
     * POST /api/v1/admin/documents/{id}/reindex — manually trigger re-indexing.
     * Returns the updated document; index result reflected in status field.
     */
    @Transactional
    public StudyDocumentJpaEntity reindexDocument(UUID id) {
        StudyDocumentJpaEntity doc = findDocument(id);
        triggerIndexing(doc);
        return documentRepository.save(doc);
    }

    // ---- Study materials pool (AC-DOC-01) --------------------------------

    /**
     * Returns only INDEXED documents (AC-DOC-01 — un-indexed docs not in study materials).
     * AC-DOC-01: content is retrievable only when status=INDEXED.
     */
    @Transactional(readOnly = true)
    public List<StudyDocumentJpaEntity> getIndexedDocuments() {
        return documentRepository.findByStatus(STATUS_INDEXED);
    }

    /** AC-DOC-01: checks whether a specific document is in the study materials pool. */
    @Transactional(readOnly = true)
    public boolean isIndexed(UUID id) {
        return documentRepository.findById(id)
                .map(doc -> STATUS_INDEXED.equals(doc.getStatus()))
                .orElse(false);
    }

    // ---- Internal helpers -------------------------------------------------

    /**
     * Calls the AI service to index the document (FR-33, design §4.5).
     * On any failure (exception or false return), records INDEX_FAILED and
     * logs the call. Never propagates the exception to the Admin (AC-DOC-02).
     *
     * <p>Always records an {@link AiCallLogService} entry (AC-LOG-01, NFR-24).
     */
    void triggerIndexing(StudyDocumentJpaEntity doc) {
        String topicCode = doc.getTopic() != null ? doc.getTopic().getCode() : null;
        long start = System.currentTimeMillis();
        boolean success = false;
        String error = null;
        try {
            success = aiServiceClient.indexDocument(
                    doc.getId(),
                    topicCode,
                    doc.getLanguage(),
                    doc.getTitle(),
                    doc.getContentMd()
            );
        } catch (Exception ex) {
            error = ex.getMessage();
        }
        int latencyMs = (int) (System.currentTimeMillis() - start);

        if (success) {
            doc.setStatus(STATUS_INDEXED);
            doc.setIndexError(null);
        } else {
            doc.setStatus(STATUS_INDEX_FAILED);
            doc.setIndexError(error != null ? error : "Indexing failed");
        }

        // AC-LOG-01: log every /ai/v1/index/* call.
        aiCallLogService.record(
                LOG_PURPOSE,
                "mock",   // provider name — mock until real HTTP client is wired in
                null,
                success ? "SUCCESS" : "FAILED",
                latencyMs,
                error
        );
    }

    private StudyDocumentJpaEntity findDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, DOCUMENT_NOT_FOUND));
    }

    private TopicJpaEntity findTopic(UUID id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, TOPIC_NOT_FOUND));
    }
}
