package com.example.interview.api.interview;

import com.example.interview.application.interview.InterviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Candidate-facing interview endpoints (Phần 2 backend).
 *
 * <p>All paths live under {@code /api/v1}, so they are JWT-authenticated by the
 * catch-all security rule ({@code /api/v1/**} → authenticated) with no
 * {@code SecurityConfig} change required. Ownership is enforced in the service
 * layer (BR-15): a candidate can only access their own JDs, sessions and reports.
 *
 * <p>Contract mirrors {@code frontend/src/api/interviewApi.ts}.
 */
@RestController
@RequestMapping("/api/v1")
public class InterviewController {

    private final InterviewService service;

    public InterviewController(InterviewService service) {
        this.service = service;
    }

    // ---- JD analysis (FR-06) ------------------------------------------------

    /** POST /api/v1/jd/analyze */
    @PostMapping("/jd/analyze")
    public InterviewResponse.JdAnalysisDto analyze(@Valid @RequestBody InterviewRequest.AnalyzeJdRequest req,
                                                    @AuthenticationPrincipal UUID userId) {
        return service.analyzeJd(userId, req.rawText());
    }

    // ---- Sessions (FR-09/FR-24) ---------------------------------------------

    /** POST /api/v1/sessions — create a session from an analysed JD. */
    @PostMapping("/sessions")
    public InterviewResponse.SessionStateDto create(@Valid @RequestBody InterviewRequest.CreateSessionRequest req,
                                                     @AuthenticationPrincipal UUID userId) {
        return service.createSession(userId, req);
    }

    /** GET /api/v1/sessions — list the candidate's sessions (FR-24 dashboard). */
    @GetMapping("/sessions")
    public List<InterviewResponse.SessionSummaryDto> list(@AuthenticationPrincipal UUID userId) {
        return service.listSessions(userId);
    }

    /** GET /api/v1/sessions/{id}/state — live state, score-free while IN_PROGRESS (BR-20). */
    @GetMapping("/sessions/{id}/state")
    public InterviewResponse.SessionStateDto state(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        return service.getState(userId, id);
    }

    /** POST /api/v1/sessions/{id}/start — CREATED → IN_PROGRESS, asks the first question. */
    @PostMapping("/sessions/{id}/start")
    public InterviewResponse.SessionStateDto start(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        return service.startSession(userId, id);
    }

    /** POST /api/v1/sessions/{id}/turns/{turnId}/answer — idempotent answer submit (BR-18). */
    @PostMapping("/sessions/{id}/turns/{turnId}/answer")
    public InterviewResponse.SessionStateDto answer(@PathVariable UUID id,
                                                     @PathVariable UUID turnId,
                                                     @Valid @RequestBody InterviewRequest.AnswerRequest req,
                                                     @AuthenticationPrincipal UUID userId) {
        return service.submitAnswer(userId, id, turnId, req.requestId(), req.answerText());
    }

    /** POST /api/v1/sessions/{id}/turns/{turnId}/skip — skip the current question (BR-12 = 0). */
    @PostMapping("/sessions/{id}/turns/{turnId}/skip")
    public InterviewResponse.SessionStateDto skip(@PathVariable UUID id,
                                                  @PathVariable UUID turnId,
                                                  @Valid @RequestBody InterviewRequest.SkipRequest req,
                                                  @AuthenticationPrincipal UUID userId) {
        return service.skipTurn(userId, id, turnId, req.requestId());
    }

    /** POST /api/v1/sessions/{id}/end — end the session early (FR-14). */
    @PostMapping("/sessions/{id}/end")
    public InterviewResponse.EndSessionDto end(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        return service.endSession(userId, id);
    }

    /** GET /api/v1/sessions/{id}/report — full report; refused until the session ends (BR-20). */
    @GetMapping("/sessions/{id}/report")
    public InterviewResponse.SessionReportDto report(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        return service.getReport(userId, id);
    }
}
