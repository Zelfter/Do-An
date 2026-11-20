package com.example.interview.api.candidate;

import com.example.interview.application.document.StudyDocumentService;
import com.example.interview.infrastructure.persistence.StudyDocumentJpaEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Read-only study-document endpoints for authenticated users. */
@RestController
@RequestMapping({"/api/documents", "/api/v1/documents"})
public class CandidateDocumentController {

    private final StudyDocumentService documentService;

    public CandidateDocumentController(StudyDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<CandidateDocumentDto> listDocuments(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String keyword) {
        return documentService.listIndexedDocuments(topic, keyword).stream()
                .map(CandidateDocumentDto::summaryFrom)
                .toList();
    }

    @GetMapping("/{id}")
    public CandidateDocumentDto getDocument(@PathVariable UUID id) {
        StudyDocumentJpaEntity document = documentService.getIndexedDocumentById(id);
        return CandidateDocumentDto.detailFrom(document);
    }
}
