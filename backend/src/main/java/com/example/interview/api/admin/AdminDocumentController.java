package com.example.interview.api.admin;

import com.example.interview.application.document.StudyDocumentService;
import com.example.interview.infrastructure.persistence.StudyDocumentJpaEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin study document (study material) endpoints (design §3.4, FR-33).
 *
 * <p>All routes live under {@code /api/v1/admin/documents} and are protected by
 * {@code hasRole(ADMIN)} in
 * {@link com.example.interview.infrastructure.security.SecurityConfig}
 * (FR-RBAC, NFR-19). A Candidate JWT yields 403; no JWT yields 401.
 *
 * <ul>
 *   <li>GET    /api/v1/admin/documents              → 200 [DocumentSummaryDto]</li>
 *   <li>POST   /api/v1/admin/documents              → 201 DocumentDto (+auto-trigger indexing)</li>
 *   <li>GET    /api/v1/admin/documents/{id}         → 200 DocumentDto | 404</li>
 *   <li>PUT    /api/v1/admin/documents/{id}         → 200 DocumentDto</li>
 *   <li>DELETE /api/v1/admin/documents/{id}         → 204</li>
 *   <li>POST   /api/v1/admin/documents/{id}/reindex → 202 ReindexResult</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/documents")
public class AdminDocumentController {

    private final StudyDocumentService documentService;

    public AdminDocumentController(StudyDocumentService documentService) {
        this.documentService = documentService;
    }

    /** Lists all documents (summary without contentMd). */
    @GetMapping
    public List<DocumentResponse.DocumentSummaryDto> listDocuments() {
        return documentService.listDocuments().stream()
                .map(DocumentResponse.DocumentSummaryDto::from)
                .toList();
    }

    /**
     * Creates a new document and auto-triggers indexing (FR-33).
     * Returns 201 regardless of indexing outcome — status field reflects
     * INDEXED or INDEX_FAILED (AC-DOC-02).
     */
    @PostMapping
    public ResponseEntity<DocumentResponse.DocumentDto> createDocument(
            @Valid @RequestBody DocumentRequest.DocumentCreateRequest request) {
        StudyDocumentJpaEntity doc = documentService.createDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DocumentResponse.DocumentDto.from(doc));
    }

    /** Fetches a single document with full content. */
    @GetMapping("/{id}")
    public DocumentResponse.DocumentDto getDocument(@PathVariable UUID id) {
        return DocumentResponse.DocumentDto.from(documentService.getDocumentById(id));
    }

    /** Full update — does not auto-reindex (call /reindex to reindex after content update). */
    @PutMapping("/{id}")
    public DocumentResponse.DocumentDto updateDocument(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentRequest.DocumentUpdateRequest request) {
        return DocumentResponse.DocumentDto.from(documentService.updateDocument(id, request));
    }

    /** Deletes a document → 204 | 404. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manually triggers (re-)indexing for an existing document (FR-33).
     * Returns 202 Accepted with the updated status (INDEXED or INDEX_FAILED).
     */
    @PostMapping("/{id}/reindex")
    public ResponseEntity<DocumentResponse.ReindexResult> reindexDocument(@PathVariable UUID id) {
        StudyDocumentJpaEntity doc = documentService.reindexDocument(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(DocumentResponse.ReindexResult.from(doc));
    }
}
