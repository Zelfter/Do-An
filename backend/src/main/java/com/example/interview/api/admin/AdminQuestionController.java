package com.example.interview.api.admin;

import com.example.interview.application.question.QuestionService;
import com.example.interview.infrastructure.persistence.QuestionJpaEntity;
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
 * Admin question-bank endpoints (design §3.3, FR-32).
 *
 * <p>All routes live under {@code /api/v1/admin/questions} and are protected by
 * {@code hasRole(ADMIN)} in
 * {@link com.example.interview.infrastructure.security.SecurityConfig}
 * (FR-RBAC, NFR-19). A Candidate JWT yields 403; no JWT yields 401.
 *
 * <ul>
 *   <li>GET    /api/v1/admin/questions              → 200 [QuestionSummaryDto]</li>
 *   <li>POST   /api/v1/admin/questions              → 201 QuestionDetailDto</li>
 *   <li>GET    /api/v1/admin/questions/{id}         → 200 QuestionDetailDto | 404</li>
 *   <li>PUT    /api/v1/admin/questions/{id}         → 200 QuestionDetailDto</li>
 *   <li>DELETE /api/v1/admin/questions/{id}         → 204</li>
 *   <li>POST   /api/v1/admin/questions/{id}/publish → 200 PublishResult | 422 {missingFields}</li>
 *   <li>POST   /api/v1/admin/questions/{id}/archive → 200 QuestionDetailDto</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/questions")
public class AdminQuestionController {

    private final QuestionService questionService;

    public AdminQuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /** Lists all questions (FR-32). */
    @GetMapping
    public List<QuestionResponse.QuestionSummaryDto> listQuestions() {
        return questionService.listQuestions().stream()
                .map(QuestionResponse.QuestionSummaryDto::from)
                .toList();
    }

    /** Creates a new DRAFT question → 201 | 404 (topic) | 400 (invalid). */
    @PostMapping
    public ResponseEntity<QuestionResponse.QuestionDetailDto> createQuestion(
            @Valid @RequestBody QuestionRequest.QuestionCreateRequest request) {
        QuestionJpaEntity question = questionService.createQuestion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(QuestionResponse.QuestionDetailDto.from(question));
    }

    /** Fetches a single question with key points → 404 when not found. */
    @GetMapping("/{id}")
    public QuestionResponse.QuestionDetailDto getQuestion(@PathVariable UUID id) {
        QuestionJpaEntity question = questionService.getQuestionById(id);
        return QuestionResponse.QuestionDetailDto.from(question);
    }

    /** Full update (FR-32). */
    @PutMapping("/{id}")
    public QuestionResponse.QuestionDetailDto updateQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody QuestionRequest.QuestionUpdateRequest request) {
        QuestionJpaEntity question = questionService.updateQuestion(id, request);
        return QuestionResponse.QuestionDetailDto.from(question);
    }

    /** Deletes a question → 204 | 404. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * AC-Q-03: publish a question → 200 PublishResult.
     * AC-Q-01 (≡ AC-18): missing required fields → 422 {missingFields:[...]}.
     */
    @PostMapping("/{id}/publish")
    public QuestionResponse.PublishResult publishQuestion(@PathVariable UUID id) {
        QuestionJpaEntity question = questionService.publishQuestion(id);
        return QuestionResponse.PublishResult.from(question, true);
    }

    /** Archives a question — removes it from the RAG pool (AC-Q-02). */
    @PostMapping("/{id}/archive")
    public QuestionResponse.QuestionDetailDto archiveQuestion(@PathVariable UUID id) {
        QuestionJpaEntity question = questionService.archiveQuestion(id);
        return QuestionResponse.QuestionDetailDto.from(question);
    }
}
