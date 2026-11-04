package com.example.interview.application.interview;

import com.example.interview.api.common.ApiException;
import com.example.interview.api.interview.InterviewRequest;
import com.example.interview.api.interview.InterviewResponse;
import com.example.interview.application.document.AiCallLogService;
import com.example.interview.application.port.AiResults;
import com.example.interview.application.port.AiServiceClient;
import com.example.interview.infrastructure.persistence.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Application service for the candidate interview flow (Phần 2 backend).
 *
 * <p>Java owns all business state: session transitions (BR-16), the
 * server-authoritative timer (NFR-16), question selection from the persisted pool
 * (FR-15/FR-22), scoring calculation (BR-01), follow-up eligibility (BR-05),
 * aggregate scores, weak-topic detection (BR-14) and reporting state (FR-43).
 * Python only supplies JD terms, criterion suggestions, follow-up text and report
 * prose via {@link AiServiceClient} (default: deterministic mock — no paid LLM).
 *
 * <p>BR-20/AC-05/AC-12: while a session is IN_PROGRESS, no candidate-facing
 * response exposes score, criterion values or feedback. Scores are only returned
 * by {@link #getReport(UUID, UUID)}, refused until the session has ended.
 */
@Service
public class InterviewService {

    private static final List<String> VALID_LEVELS = List.of("JUNIOR", "MID", "SENIOR");
    private static final List<String> ENDED_STATUSES = List.of("COMPLETED", "TIMED_OUT", "CANCELLED");

    private final InterviewSessionJpaRepository sessionRepo;
    private final SessionTurnJpaRepository turnRepo;
    private final TurnEvaluationJpaRepository evalRepo;
    private final SessionReportJpaRepository reportRepo;
    private final JobDescriptionJpaRepository jdRepo;
    private final JdTopicJpaRepository jdTopicRepo;
    private final SessionQuestionPoolJpaRepository poolRepo;
    private final SessionTopicJpaRepository sessionTopicRepo;
    private final QuestionJpaRepository questionRepo;
    private final TopicJpaRepository topicRepo;
    private final UserJpaRepository userRepo;
    private final StudyDocumentJpaRepository studyDocRepo;
    private final AiServiceClient aiClient;
    private final AiCallLogService aiCallLogService;
    private final ScoringConfigService scoringConfig;
    private final ObjectMapper objectMapper;

    public InterviewService(InterviewSessionJpaRepository sessionRepo,
                            SessionTurnJpaRepository turnRepo,
                            TurnEvaluationJpaRepository evalRepo,
                            SessionReportJpaRepository reportRepo,
                            JobDescriptionJpaRepository jdRepo,
                            JdTopicJpaRepository jdTopicRepo,
                            SessionQuestionPoolJpaRepository poolRepo,
                            SessionTopicJpaRepository sessionTopicRepo,
                            QuestionJpaRepository questionRepo,
                            TopicJpaRepository topicRepo,
                            UserJpaRepository userRepo,
                            StudyDocumentJpaRepository studyDocRepo,
                            AiServiceClient aiClient,
                            AiCallLogService aiCallLogService,
                            ScoringConfigService scoringConfig,
                            ObjectMapper objectMapper) {
        this.sessionRepo = sessionRepo;
        this.turnRepo = turnRepo;
        this.evalRepo = evalRepo;
        this.reportRepo = reportRepo;
        this.jdRepo = jdRepo;
        this.jdTopicRepo = jdTopicRepo;
        this.poolRepo = poolRepo;
        this.sessionTopicRepo = sessionTopicRepo;
        this.questionRepo = questionRepo;
        this.topicRepo = topicRepo;
        this.userRepo = userRepo;
        this.studyDocRepo = studyDocRepo;
        this.aiClient = aiClient;
        this.aiCallLogService = aiCallLogService;
        this.scoringConfig = scoringConfig;
        this.objectMapper = objectMapper;
    }

    // =========================================================================
    // JD analysis (FR-06)
    // =========================================================================

    @Transactional
    public InterviewResponse.JdAnalysisDto analyzeJd(UUID userId, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nội dung JD không được để trống.");
        }
        UserJpaEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));

        JobDescriptionJpaEntity jd = new JobDescriptionJpaEntity();
        jd.setUser(user);
        jd.setRawText(rawText);
        jd.setStatus("ANALYZING");
        jd.setLevel(null);
        jd = jdRepo.save(jd);

        AiResults.JdAnalysisResult result;
        long start = System.currentTimeMillis();
        try {
            result = aiClient.analyzeJd(rawText);
            aiCallLogService.record("JD_ANALYZE", "mock", null, "SUCCESS",
                    (int) (System.currentTimeMillis() - start), null);
        } catch (Exception ex) {
            aiCallLogService.record("JD_ANALYZE", "mock", null, "FAILED",
                    (int) (System.currentTimeMillis() - start), ex.getMessage());
            jd.setStatus("FAILED");
            jdRepo.save(jd);
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Phân tích JD thất bại. Vui lòng thử lại.");
        }

        String level = (result != null && result.level() != null) ? result.level() : "MID";
        if (!VALID_LEVELS.contains(level)) {
            level = "MID";
        }
        jd.setLevel(level);

        List<TopicJpaEntity> allTopics = topicRepo.findAll();
        List<InterviewResponse.JdTopicDto> termDtos = new ArrayList<>();
        if (result != null && result.terms() != null) {
            for (AiResults.JdAnalysisTerm term : result.terms()) {
                UUID topicId = matchTopic(term.rawTerm(), allTopics);
                TopicJpaEntity topic = topicId != null ? topicRepo.findById(topicId).orElse(null) : null;
                JdTopicJpaEntity jt = new JdTopicJpaEntity();
                jt.setJobDescription(jd);
                jt.setTopic(topic);
                jt.setRawTerm(term.rawTerm());
                jt.setImportance(term.importance() != null ? term.importance() : "PREFERRED");
                jt.setSelected(true);
                jdTopicRepo.save(jt);
                termDtos.add(new InterviewResponse.JdTopicDto(
                        jt.getId().toString(), term.rawTerm(),
                        topicId != null ? topicId.toString() : null,
                        topic != null ? topic.getCode() : null,
                        jt.getImportance(), true));
            }
        }
        jd.setStatus("READY");
        jdRepo.save(jd);
        return new InterviewResponse.JdAnalysisDto(jd.getId().toString(), level, termDtos);
    }

    // =========================================================================
    // Session creation (FR-09, FR-15)
    // =========================================================================

    @Transactional
    public InterviewResponse.SessionStateDto createSession(UUID userId, InterviewRequest.CreateSessionRequest req) {
        JobDescriptionJpaEntity jd = jdRepo.findById(req.jdId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy JD."));
        if (!jd.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập JD này.");
        }
        if (!"READY".equals(jd.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "JD chưa được phân tích xong.");
        }

        String level = (req.level() != null && !req.level().isBlank()) ? req.level()
                : (jd.getLevel() != null ? jd.getLevel() : "MID");
        validateLevel(level);
        jd.setLevel(level);

        int timeLimit = req.timeLimitSec() != null ? req.timeLimitSec() : 3600;
        if (timeLimit < 60 || timeLimit > 7200) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thời lượng phải từ 1 đến 120 phút.");
        }

        List<UUID> selected = req.selectedTopicIds() == null ? List.of() : req.selectedTopicIds();
        if (selected.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ít nhất một chủ đề.");
        }

        // Map JD topics -> importance for weight assignment + validation.
        List<JdTopicJpaEntity> jdTopics = jdTopicRepo.findByJobDescription_Id(jd.getId());
        Map<UUID, String> topicImportance = new HashMap<>();
        for (JdTopicJpaEntity jt : jdTopics) {
            if (jt.getTopic() != null) {
                topicImportance.put(jt.getTopic().getId(), jt.getImportance());
            }
        }
        for (UUID tid : selected) {
            if (!topicImportance.containsKey(tid)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Chủ đề được chọn không thuộc JD.");
            }
        }

        // FR-15: build the question pool ONCE from PUBLISHED+INDEXED questions.
        List<QuestionJpaEntity> pool = new ArrayList<>();
        for (QuestionJpaEntity q : questionRepo.findByStatusAndIndexStatus("PUBLISHED", "INDEXED")) {
            if (q.getTopic() != null && selected.contains(q.getTopic().getId())
                    && pool.stream().noneMatch(x -> x.getId().equals(q.getId()))) {
                pool.add(q);
            }
        }
        if (pool.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không có câu hỏi phù hợp trong ngân hàng cho các chủ đề đã chọn. "
                            + "Vui lòng chọn chủ đề khác hoặc chờ quản trị viên bổ sung câu hỏi.");
        }

        InterviewSessionJpaEntity session = new InterviewSessionJpaEntity();
        session.setUser(jd.getUser());
        session.setJobDescription(jd);
        session.setStatus("CREATED");
        session.setTimeLimitSec(timeLimit);
        session.setElapsedMs(0);
        session.setConfigSnapshot(scoringConfig.snapshotJson());
        session.setTotalScore(null);
        session.setResumeAt(null);
        session = sessionRepo.save(session);
        jdRepo.save(jd);

        ScoringConfigService.ScoringConfig cfg = scoringConfig.current();
        for (UUID tid : selected) {
            SessionTopicJpaEntity t = new SessionTopicJpaEntity();
            t.setSessionId(session.getId());
            t.setTopicId(tid);
            BigDecimal weight = "REQUIRED".equals(topicImportance.get(tid))
                    ? cfg.requiredSkillMultiplier() : BigDecimal.ONE;
            t.setWeight(weight);
            sessionTopicRepo.save(t);
        }

        int rank = 0;
        for (QuestionJpaEntity q : pool) {
            SessionQuestionPoolJpaEntity e = new SessionQuestionPoolJpaEntity();
            e.setSessionId(session.getId());
            e.setQuestionId(q.getId());
            e.setRank(rank++);
            e.setUsed(false);
            poolRepo.save(e);
        }

        return buildState(session);
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse.SessionSummaryDto> listSessions(UUID userId) {
        return sessionRepo.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(s -> new InterviewResponse.SessionSummaryDto(
                        s.getId().toString(),
                        s.getStatus(),
                        s.getJobDescription().getLevel() != null ? s.getJobDescription().getLevel() : "MID",
                        s.getTotalScore() != null ? s.getTotalScore().doubleValue() : null,
                        s.getCreatedAt() != null ? s.getCreatedAt().toString() : null))
                .toList();
    }

    // =========================================================================
    // Session lifecycle (FR-09, FR-14, FR-16, BR-16)
    // =========================================================================

    @Transactional
    public InterviewResponse.SessionStateDto startSession(UUID userId, UUID sessionId) {
        InterviewSessionJpaEntity session = ownedSession(userId, sessionId);
        if (!"CREATED".equals(session.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Phiên phỏng vấn đã bắt đầu.");
        }
        session.setStatus("IN_PROGRESS");
        session.setResumeAt(Instant.now());
        sessionRepo.save(session);

        if (askNext(session) == null) {
            // No questions available at all → end immediately.
            return finishSession(session, "COMPLETED");
        }
        return buildState(session);
    }

    @Transactional
    public InterviewResponse.SessionStateDto getState(UUID userId, UUID sessionId) {
        InterviewSessionJpaEntity session = ownedSession(userId, sessionId);
        maybeTimeout(session); // server-authoritative timer (NFR-16)
        return buildState(session);
    }

    @Transactional
    public InterviewResponse.SessionStateDto submitAnswer(UUID userId, UUID sessionId,
                                                           UUID turnId, String requestId, String answerText) {
        InterviewSessionJpaEntity session = ownedSession(userId, sessionId);
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Phiên phỏng vấn đã kết thúc.");
        }
        SessionTurnJpaEntity turn = turnRepo.findById(turnId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi."));
        if (!turn.getSession().getId().equals(session.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Câu hỏi không thuộc phiên này.");
        }
        // Idempotent replay (BR-18): an already-processed turn is not re-submitted.
        if (!"PENDING".equals(turn.getStatus())) {
            return buildState(session);
        }
        if (answerText == null || answerText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Câu trả lời không được để trống.");
        }

        accumulateElapsed(session); // candidate's answering window; timer pauses for grading

        turn.setAnswerText(answerText); // BR-17: immutable once set
        turn.setStatus("ANSWERED");
        turnRepo.save(turn);

        QuestionJpaEntity q = turn.getQuestion();
        String refAnswer = q != null ? q.getReferenceAnswer() : "";
        List<String> keyPoints = q != null
                ? q.getKeyPoints().stream().map(QuestionKeyPointJpaEntity::getContent).toList()
                : List.of();
        ScoringConfigService.ScoringConfig cfg = scoringConfig.fromSnapshot(session.getConfigSnapshot());

        AiResults.TurnEvaluationResult result = callAi("EVALUATE_TURN",
                () -> aiClient.evaluateTurn(turn.getQuestionText(), refAnswer, keyPoints,
                        answerText, session.getJobDescription().getLevel()),
                null);

        if (result != null) {
            BigDecimal acc = clamp(result.accuracy());
            BigDecimal comp = clamp(result.completeness());
            BigDecimal cla = clamp(result.clarity());
            BigDecimal ex = clamp(result.exampleScore());
            BigDecimal total = cfg.total(acc, comp, cla, ex);

            TurnEvaluationJpaEntity eval = new TurnEvaluationJpaEntity();
            eval.setTurn(turn);
            eval.setAccuracy(acc);
            eval.setCompleteness(comp);
            eval.setClarity(cla);
            eval.setExampleScore(ex);
            eval.setTotalScore(total);
            eval.setMissedKeyPoints(toJson(result.missedKeyPoints() != null ? result.missedKeyPoints() : List.of()));
            eval.setFeedback(null); // BR-20: null until the report is generated
            evalRepo.save(eval);

            // Follow-up eligibility (BR-05): score% < trigger threshold AND consecutive < max.
            int scorePct = cfg.pct(total);
            int consec = consecutiveFollowUps(turn);
            if (scorePct < cfg.followUpTriggerThreshold()
                    && consec < cfg.followUpMaxConsecutive()
                    && result.followUpQuestion() != null && !result.followUpQuestion().isBlank()) {
                SessionTurnJpaEntity fu = new SessionTurnJpaEntity();
                fu.setSession(session);
                fu.setParentTurn(turn);
                fu.setKind("FOLLOW_UP");
                fu.setQuestionText(result.followUpQuestion());
                fu.setStatus("PENDING");
                turnRepo.save(fu);
                session.setResumeAt(Instant.now());
                sessionRepo.save(session);
                return buildState(session);
            }
        } else {
            // AI failed after retries → UNGRADED (BR-18). Not treated as zero; no follow-up.
            turn.setStatus("UNGRADED");
            turnRepo.save(turn);
        }

        return advanceOrEnd(session);
    }

    @Transactional
    public InterviewResponse.SessionStateDto skipTurn(UUID userId, UUID sessionId, UUID turnId, String requestId) {
        InterviewSessionJpaEntity session = ownedSession(userId, sessionId);
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Phiên phỏng vấn đã kết thúc.");
        }
        SessionTurnJpaEntity turn = turnRepo.findById(turnId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi."));
        if (!turn.getSession().getId().equals(session.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Câu hỏi không thuộc phiên này.");
        }
        if (!"PENDING".equals(turn.getStatus())) {
            return buildState(session);
        }

        accumulateElapsed(session);
        turn.setStatus("SKIPPED");
        turnRepo.save(turn);

        // BR-12: skipped = 0 → store a zero evaluation so it counts in the topic average.
        TurnEvaluationJpaEntity eval = new TurnEvaluationJpaEntity();
        eval.setTurn(turn);
        eval.setAccuracy(BigDecimal.ZERO);
        eval.setCompleteness(BigDecimal.ZERO);
        eval.setClarity(BigDecimal.ZERO);
        eval.setExampleScore(BigDecimal.ZERO);
        eval.setTotalScore(BigDecimal.ZERO);
        eval.setMissedKeyPoints("[]");
        eval.setFeedback(null);
        evalRepo.save(eval);

        return advanceOrEnd(session);
    }

    @Transactional
    public InterviewResponse.EndSessionDto endSession(UUID userId, UUID sessionId) {
        InterviewSessionJpaEntity session = ownedSession(userId, sessionId);
        if (!"IN_PROGRESS".equals(session.getStatus()) && !"CREATED".equals(session.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Phiên phỏng vấn đã kết thúc.");
        }
        if ("CREATED".equals(session.getStatus())) {
            session.setStatus("CANCELLED");
            session.setResumeAt(null);
            sessionRepo.save(session);
            return new InterviewResponse.EndSessionDto(session.getId().toString(), "CANCELLED", Instant.now().toString());
        }
        accumulateElapsed(session);
        finishSession(session, "COMPLETED");
        return new InterviewResponse.EndSessionDto(session.getId().toString(), "COMPLETED", Instant.now().toString());
    }

    @Transactional
    public InterviewResponse.SessionReportDto getReport(UUID userId, UUID sessionId) {
        InterviewSessionJpaEntity session = ownedSession(userId, sessionId);
        if (!isEnded(session.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Báo cáo chỉ có sẵn sau khi kết thúc phỏng vấn.");
        }
        SessionReportJpaEntity report = reportRepo.findById(session.getId()).orElse(null);
        if (report == null) {
            generateReport(session);
            report = reportRepo.findById(session.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo báo cáo."));
        }

        ScoringConfigService.ScoringConfig cfg = scoringConfig.fromSnapshot(session.getConfigSnapshot());
        int totalPct = session.getTotalScore() != null ? cfg.pct(session.getTotalScore()) : 0;
        String classification = cfg.classification(totalPct);

        List<SessionTopicJpaEntity> st = sessionTopicRepo.findBySessionId(session.getId());
        Map<UUID, String> nameCache = new HashMap<>();
        List<InterviewResponse.TopicScoreDto> topics = st.stream().map(t -> {
            TopicJpaEntity topic = topicRepo.findById(t.getTopicId()).orElse(null);
            return new InterviewResponse.TopicScoreDto(
                    t.getTopicId().toString(),
                    topic != null ? topic.getCode() : null,
                    viName(topic, nameCache),
                    t.getTopicScore() != null ? t.getTopicScore().doubleValue() : null,
                    Boolean.TRUE.equals(t.getIsWeak()));
        }).toList();
        List<InterviewResponse.TopicScoreDto> weak = topics.stream()
                .filter(InterviewResponse.TopicScoreDto::isWeak).toList();

        List<SessionTurnJpaEntity> turns = turnRepo.findBySession_IdOrderByCreatedAtAsc(session.getId());
        Map<UUID, TurnEvaluationJpaEntity> evals = new HashMap<>();
        for (TurnEvaluationJpaEntity e : evalRepo.findByTurn_Session_Id(session.getId())) {
            evals.put(e.getTurnId(), e);
        }
        List<InterviewResponse.TurnScoreDto> turnDtos = turns.stream().map(t -> {
            TurnEvaluationJpaEntity ev = evals.get(t.getId());
            if (ev != null) {
                return new InterviewResponse.TurnScoreDto(
                        t.getId().toString(), t.getKind(), t.getQuestionText(), t.getAnswerText(), t.getStatus(),
                        ev.getAccuracy().doubleValue(), ev.getCompleteness().doubleValue(),
                        ev.getClarity().doubleValue(), ev.getExampleScore().doubleValue(),
                        ev.getTotalScore().doubleValue(),
                        fromJsonStringArray(ev.getMissedKeyPoints()), ev.getFeedback());
            }
            return new InterviewResponse.TurnScoreDto(
                    t.getId().toString(), t.getKind(), t.getQuestionText(), t.getAnswerText(), t.getStatus(),
                    null, null, null, null, null, List.of(), null);
        }).toList();

        // Study materials for weak topics (FR-25).
        List<StudyDocumentJpaEntity> indexed = studyDocRepo.findByStatus("INDEXED");
        List<InterviewResponse.StudyMaterialDto> materials = new ArrayList<>();
        for (InterviewResponse.TopicScoreDto w : weak) {
            for (StudyDocumentJpaEntity d : indexed) {
                if (d.getTopic() != null && d.getTopic().getId().toString().equals(w.topicId())) {
                    materials.add(new InterviewResponse.StudyMaterialDto(
                            w.topicId(), w.name(), d.getId().toString(), d.getTitle()));
                }
            }
        }

        String level = session.getJobDescription().getLevel() != null
                ? session.getJobDescription().getLevel() : "MID";
        return new InterviewResponse.SessionReportDto(
                session.getId().toString(), session.getStatus(), report.getStatus(),
                level, session.getTotalScore() != null ? session.getTotalScore().doubleValue() : 0,
                classification, topics, weak, turnDtos, report.getSummary(),
                materials, report.getGeneratedAt() != null ? report.getGeneratedAt().toString() : null);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private InterviewResponse.SessionStateDto advanceOrEnd(InterviewSessionJpaEntity session) {
        if (askNext(session) != null) {
            session.setResumeAt(Instant.now());
            sessionRepo.save(session);
            return buildState(session);
        }
        return finishSession(session, "COMPLETED");
    }

    /** Asks the next unused ORIGINAL question from the pool (FR-15/FR-22). */
    private SessionTurnJpaEntity askNext(InterviewSessionJpaEntity session) {
        List<SessionQuestionPoolJpaEntity> unused = poolRepo.findBySessionIdAndUsedFalseOrderByRankAsc(session.getId());
        if (unused.isEmpty()) {
            return null;
        }
        SessionQuestionPoolJpaEntity entry = unused.get(0);
        QuestionJpaEntity q = questionRepo.findById(entry.getQuestionId()).orElse(null);
        if (q == null) {
            entry.setUsed(true);
            poolRepo.save(entry);
            return askNext(session); // skip deleted question
        }
        entry.setUsed(true);
        poolRepo.save(entry);
        SessionTurnJpaEntity turn = new SessionTurnJpaEntity();
        turn.setSession(session);
        turn.setKind("ORIGINAL");
        turn.setQuestion(q);
        turn.setQuestionText(q.getContent());
        turn.setStatus("PENDING");
        return turnRepo.save(turn);
    }

    /** Ends the session: finalise scores, then generate the report. */
    private InterviewResponse.SessionStateDto finishSession(InterviewSessionJpaEntity session, String status) {
        session.setStatus(status);
        session.setResumeAt(null);
        finalizeScores(session);
        sessionRepo.save(session);
        generateReport(session);
        return buildState(session);
    }

    /** Detects server-side timeout and finalises as TIMED_OUT (NFR-16, BR-09). */
    private void maybeTimeout(InterviewSessionJpaEntity session) {
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            return;
        }
        List<SessionTurnJpaEntity> turns = turnRepo.findBySession_IdOrderByCreatedAtAsc(session.getId());
        boolean hasPending = turns.stream().anyMatch(t -> "PENDING".equals(t.getStatus()));
        if (remainingSec(session, hasPending) > 0) {
            return;
        }
        accumulateElapsed(session);
        long limitMs = session.getTimeLimitSec() * 1000L;
        if (session.getElapsedMs() > limitMs) {
            session.setElapsedMs(limitMs);
        }
        for (SessionTurnJpaEntity t : turns) {
            if ("PENDING".equals(t.getStatus())) {
                t.setStatus("TIMED_OUT");
                turnRepo.save(t);
                // BR-12: time-out = 0
                TurnEvaluationJpaEntity eval = new TurnEvaluationJpaEntity();
                eval.setTurn(t);
                eval.setAccuracy(BigDecimal.ZERO);
                eval.setCompleteness(BigDecimal.ZERO);
                eval.setClarity(BigDecimal.ZERO);
                eval.setExampleScore(BigDecimal.ZERO);
                eval.setTotalScore(BigDecimal.ZERO);
                eval.setMissedKeyPoints("[]");
                eval.setFeedback(null);
                evalRepo.save(eval);
            }
        }
        session.setStatus("TIMED_OUT");
        finalizeScores(session);
        sessionRepo.save(session);
        generateReport(session);
    }

    /** BR-10/BR-11/BR-14: per-topic scores, weak flags, and weighted session total. */
    private void finalizeScores(InterviewSessionJpaEntity session) {
        ScoringConfigService.ScoringConfig cfg = scoringConfig.fromSnapshot(session.getConfigSnapshot());
        List<SessionTurnJpaEntity> turns = turnRepo.findBySession_IdOrderByCreatedAtAsc(session.getId());
        Map<UUID, TurnEvaluationJpaEntity> evals = new HashMap<>();
        for (TurnEvaluationJpaEntity e : evalRepo.findByTurn_Session_Id(session.getId())) {
            evals.put(e.getTurnId(), e);
        }
        List<SessionTopicJpaEntity> st = sessionTopicRepo.findBySessionId(session.getId());

        for (SessionTopicJpaEntity t : st) {
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            int weakAnswers = 0;
            for (SessionTurnJpaEntity turn : turns) {
                TopicJpaEntity topic = turnTopic(turn);
                if (topic == null || !topic.getId().equals(t.getTopicId())) {
                    continue;
                }
                TurnEvaluationJpaEntity ev = evals.get(turn.getId());
                if (ev == null) {
                    continue; // UNGRADED turns are excluded, NOT treated as zero
                }
                sum = sum.add(ev.getTotalScore());
                count++;
                if (cfg.pct(ev.getTotalScore()) < cfg.mediumThreshold()) {
                    weakAnswers++;
                }
            }
            if (count > 0) {
                BigDecimal topicScore = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
                t.setTopicScore(topicScore);
                int pct = cfg.pct(topicScore);
                t.setIsWeak(pct < cfg.weakTopicThreshold() || weakAnswers >= cfg.minWeakAnswers());
            } else {
                t.setTopicScore(null);
                t.setIsWeak(null);
            }
            sessionTopicRepo.save(t);
        }

        BigDecimal weightSum = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;
        for (SessionTopicJpaEntity t : st) {
            if (t.getTopicScore() == null) {
                continue;
            }
            weightedSum = weightedSum.add(t.getTopicScore().multiply(t.getWeight()));
            weightSum = weightSum.add(t.getWeight());
        }
        BigDecimal total = weightSum.compareTo(BigDecimal.ZERO) > 0
                ? weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        session.setTotalScore(total);
    }

    /** FR-43/AC-22: generate the report prose; fall back to PARTIAL (keep scores) on failure. */
    private void generateReport(InterviewSessionJpaEntity session) {
        ScoringConfigService.ScoringConfig cfg = scoringConfig.fromSnapshot(session.getConfigSnapshot());
        int totalPct = session.getTotalScore() != null ? cfg.pct(session.getTotalScore()) : 0;
        String classification = cfg.classification(totalPct);

        List<SessionTopicJpaEntity> st = sessionTopicRepo.findBySessionId(session.getId());
        Map<UUID, String> nameCache = new HashMap<>();
        List<String> weakNames = st.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsWeak()))
                .map(t -> viName(topicRepo.findById(t.getTopicId()).orElse(null), nameCache))
                .filter(Objects::nonNull).toList();

        long answered = turnRepo.findBySession_IdOrderByCreatedAtAsc(session.getId()).stream()
                .filter(x -> !"PENDING".equals(x.getStatus())).count();
        long total = poolRepo.findBySessionIdOrderByRankAsc(session.getId()).size();

        AiResults.ReportContext ctx = new AiResults.ReportContext(
                session.getJobDescription().getLevel() != null ? session.getJobDescription().getLevel() : "MID",
                (int) answered, (int) total, String.valueOf(totalPct), classification, weakNames);
        AiResults.ReportProse prose = callAi("GENERATE_REPORT", () -> aiClient.generateReport(ctx), null);

        SessionReportJpaEntity report = reportRepo.findById(session.getId()).orElseGet(() -> {
            SessionReportJpaEntity r = new SessionReportJpaEntity();
            r.setSession(session);
            return r;
        });
        if (prose != null && prose.summary() != null && !prose.summary().isBlank()) {
            report.setStatus("READY");
            report.setSummary(prose.summary());
        } else {
            report.setStatus("PARTIAL");
            report.setSummary(null); // AC-22: scores preserved even without prose
        }
        report.setGeneratedAt(Instant.now());
        reportRepo.save(report);
    }

    // ---- state building (BR-20: score-free while IN_PROGRESS) ----------------

    private InterviewResponse.SessionStateDto buildState(InterviewSessionJpaEntity session) {
        List<SessionTopicJpaEntity> st = sessionTopicRepo.findBySessionId(session.getId());
        Map<UUID, String> nameCache = new HashMap<>();
        List<InterviewResponse.SessionTopicDto> topics = st.stream().map(t -> {
            TopicJpaEntity topic = topicRepo.findById(t.getTopicId()).orElse(null);
            String name = viName(topic, nameCache);
            return new InterviewResponse.SessionTopicDto(
                    t.getTopicId().toString(),
                    topic != null ? topic.getCode() : null,
                    name != null ? name : "—");
        }).toList();

        List<SessionTurnJpaEntity> turns = turnRepo.findBySession_IdOrderByCreatedAtAsc(session.getId());
        List<InterviewResponse.TurnTranscriptDto> transcript = turns.stream().map(t -> {
            TopicJpaEntity topic = turnTopic(t);
            return new InterviewResponse.TurnTranscriptDto(
                    t.getId().toString(), t.getKind(), t.getQuestionText(),
                    "PENDING".equals(t.getStatus()) ? null : t.getAnswerText(),
                    viName(topic, nameCache),
                    topic != null ? topic.getCode() : null);
        }).toList();

        SessionTurnJpaEntity pending = turns.stream()
                .filter(t -> "PENDING".equals(t.getStatus()))
                .reduce((a, b) -> b).orElse(null); // last pending turn
        InterviewResponse.CurrentTurnDto current = null;
        int followUpCount = 0;
        if (pending != null) {
            TopicJpaEntity topic = turnTopic(pending);
            current = new InterviewResponse.CurrentTurnDto(
                    pending.getId().toString(), pending.getKind(), pending.getQuestionText(),
                    viName(topic, nameCache), topic != null ? topic.getCode() : null);
            followUpCount = consecutiveFollowUps(pending);
        }

        int poolSize = poolRepo.findBySessionIdOrderByRankAsc(session.getId()).size();
        long answered = turns.stream().filter(t -> !"PENDING".equals(t.getStatus())).count();
        int remaining = remainingSec(session, pending != null);
        String level = session.getJobDescription().getLevel() != null
                ? session.getJobDescription().getLevel() : "MID";
        String endedAt = isEnded(session.getStatus()) && session.getUpdatedAt() != null
                ? session.getUpdatedAt().toString() : null;

        return new InterviewResponse.SessionStateDto(
                session.getId().toString(), session.getStatus(), level,
                session.getTimeLimitSec(), session.getElapsedMs(), remaining,
                topics, poolSize, (int) answered, followUpCount,
                transcript, current, endedAt);
    }

    private int remainingSec(InterviewSessionJpaEntity session, boolean hasPendingTurn) {
        long limitMs = session.getTimeLimitSec() * 1000L;
        long elapsed = session.getElapsedMs();
        if (hasPendingTurn && session.getResumeAt() != null) {
            elapsed += Duration.between(session.getResumeAt(), Instant.now()).toMillis();
        }
        long remainingMs = Math.max(0, limitMs - elapsed);
        return (int) Math.min(Integer.MAX_VALUE, remainingMs / 1000);
    }

    /** Number of consecutive FOLLOW_UP turns behind this one (0 for an ORIGINAL). */
    private int consecutiveFollowUps(SessionTurnJpaEntity turn) {
        int depth = 0;
        SessionTurnJpaEntity t = turn;
        while (t != null && "FOLLOW_UP".equals(t.getKind())) {
            t = t.getParentTurn();
            depth++;
        }
        return depth;
    }

    /** The topic of the ORIGINAL question a turn belongs to (walks follow-up parents). */
    private TopicJpaEntity turnTopic(SessionTurnJpaEntity turn) {
        SessionTurnJpaEntity t = turn;
        while (t != null && t.getQuestion() == null && t.getParentTurn() != null) {
            t = t.getParentTurn();
        }
        return (t != null && t.getQuestion() != null) ? t.getQuestion().getTopic() : null;
    }

    private String viName(TopicJpaEntity topic, Map<UUID, String> cache) {
        if (topic == null) {
            return null;
        }
        return cache.computeIfAbsent(topic.getId(), id -> {
            for (TopicTranslationJpaEntity tr : topic.getTranslations()) {
                if ("vi".equalsIgnoreCase(tr.getLanguage())) {
                    return tr.getName();
                }
            }
            return topic.getCode();
        });
    }

    private UUID matchTopic(String rawTerm, List<TopicJpaEntity> allTopics) {
        String needle = rawTerm == null ? "" : rawTerm.toLowerCase(Locale.ROOT);
        if (needle.isBlank()) {
            return null;
        }
        List<String[]> cands = new ArrayList<>(); // {keywordLower, topicId}
        for (TopicJpaEntity t : allTopics) {
            if (!t.isActive()) {
                continue;
            }
            addCand(cands, t.getCode(), t.getId());
            for (TopicAliasJpaEntity a : t.getAliases()) {
                addCand(cands, a.getAlias(), t.getId());
            }
            for (TopicTranslationJpaEntity tr : t.getTranslations()) {
                if ("vi".equalsIgnoreCase(tr.getLanguage())) {
                    addCand(cands, tr.getName(), t.getId());
                }
            }
        }
        // Prefer longer (more specific) keyword matches.
        cands.sort((x, y) -> Integer.compare(y[0].length(), x[0].length()));
        for (String[] c : cands) {
            if (c[0].length() >= 3 && needle.contains(c[0])) {
                return UUID.fromString(c[1]);
            }
        }
        return null;
    }

    private void addCand(List<String[]> cands, String kw, UUID id) {
        if (kw != null && !kw.isBlank()) {
            cands.add(new String[]{kw.toLowerCase(Locale.ROOT), id.toString()});
        }
    }

    // ---- timer / utility -----------------------------------------------------

    private void accumulateElapsed(InterviewSessionJpaEntity session) {
        if (session.getResumeAt() != null) {
            long delta = Duration.between(session.getResumeAt(), Instant.now()).toMillis();
            if (delta > 0) {
                session.setElapsedMs(session.getElapsedMs() + delta);
            }
            session.setResumeAt(null); // pause while AI grades
        }
    }

    private BigDecimal clamp(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return v.min(BigDecimal.TEN).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJsonStringArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Calls the AI service with timing + logging; returns fallback on failure (never throws). */
    private <T> T callAi(String purpose, Supplier<T> action, T fallback) {
        long start = System.currentTimeMillis();
        try {
            T result = action.get();
            aiCallLogService.record(purpose, "mock", null, "SUCCESS",
                    (int) (System.currentTimeMillis() - start), null);
            return result;
        } catch (Exception ex) {
            aiCallLogService.record(purpose, "mock", null, "FAILED",
                    (int) (System.currentTimeMillis() - start), ex.getMessage());
            return fallback;
        }
    }

    private InterviewSessionJpaEntity ownedSession(UUID userId, UUID sessionId) {
        InterviewSessionJpaEntity s = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên phỏng vấn."));
        if (!s.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập phiên này.");
        }
        return s;
    }

    private void validateLevel(String level) {
        if (level == null || !VALID_LEVELS.contains(level)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cấp độ phải là JUNIOR, MID hoặc SENIOR.");
        }
    }

    private static boolean isEnded(String status) {
        return ENDED_STATUSES.contains(status);
    }
}
