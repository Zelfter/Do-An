/**
 * Interview API client — candidate-facing interview session endpoints.
 * design §3.x, FR-09/10/11/14/16/17/20/22/23, BR-01/05/16/17/18/20.
 *
 * Frontend talks ONLY to Java /api/v1 (architecture-guardrails). The shared
 * apiClient already attaches the Bearer token, so every call here is
 * authenticated. No token, no access — integration with the existing auth flow.
 *
 * IMPORTANT (BR-20 / AC-05 / AC-12): while a session is IN_PROGRESS, the server
 * omits ALL score, criterion and feedback fields from the state response.
 * Scores are only exposed by getSessionReport(), which the server refuses
 * until the session has ended.
 */
import { apiClient } from './authApi'

// ─── Types ───────────────────────────────────────────────────────────────────

export type SessionStatus = 'CREATED' | 'IN_PROGRESS' | 'COMPLETED' | 'TIMED_OUT' | 'CANCELLED'
export type TurnKind = 'ORIGINAL' | 'FOLLOW_UP'
export type Level = 'JUNIOR' | 'MID' | 'SENIOR'

export interface SessionTopicView {
  topicId: string
  code: string
  name: string
}

/** One entry in the live transcript. Deliberately carries NO score (BR-20). */
export interface TurnTranscriptView {
  turnId: string
  kind: TurnKind
  questionText: string
  /** null while the turn is still awaiting an answer (answered turns are immutable — BR-17). */
  answerText: string | null
  topicName: string | null
  topicCode: string | null
}

/** The question the candidate must answer right now, or null when ended/between turns. */
export interface CurrentTurnView {
  turnId: string
  kind: TurnKind
  questionText: string
  topicName: string | null
  topicCode: string | null
}

/**
 * Live session state. Score-free while IN_PROGRESS (BR-20).
 * `remainingSec` is server-authoritative (NFR-16): the client ticks locally
 * and re-syncs by polling getSessionState().
 */
export interface InterviewSessionState {
  sessionId: string
  status: SessionStatus
  level: Level
  timeLimitSec: number
  elapsedMs: number
  remainingSec: number
  topics: SessionTopicView[]
  totalQuestions: number
  answeredCount: number
  followUpCount: number
  turns: TurnTranscriptView[]
  currentTurn: CurrentTurnView | null
  endedAt: string | null
}

export interface EndSessionResult {
  sessionId: string
  status: SessionStatus
  endedAt: string
}

// ─── Report (scores only available AFTER the session ends) ───────────────────

export interface TopicScoreView {
  topicId: string
  code: string
  name: string
  topicScore: number
  isWeak: boolean
}

export interface TurnScoreView {
  turnId: string
  kind: TurnKind
  questionText: string
  answerText: string | null
  status: 'PENDING' | 'ANSWERED' | 'SKIPPED' | 'TIMED_OUT' | 'UNGRADED'
  accuracy: number
  completeness: number
  clarity: number
  exampleScore: number
  totalScore: number
  missedKeyPoints: string[]
  feedback: string | null
}

export interface StudyMaterialView {
  topicId: string
  topicName: string
  documentId: string
  title: string
}

export interface SessionReport {
  sessionId: string
  status: SessionStatus
  reportStatus: 'GENERATING' | 'READY' | 'PARTIAL'
  level: Level
  totalScore: number
  classification: string
  topics: TopicScoreView[]
  weakTopics: TopicScoreView[]
  turns: TurnScoreView[]
  summary: string | null
  studyMaterials: StudyMaterialView[]
  generatedAt: string | null
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Generates an idempotency id for an answer/skip attempt (used on retry — BR-18/idempotent answers). */
export function newRequestId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID()
  return 'rid-' + Math.random().toString(36).slice(2) + Date.now().toString(36)
}

// ─── Calls ────────────────────────────────────────────────────────────────────

/** GET /api/v1/sessions/{id}/state — live state, NO scores while IN_PROGRESS (BR-20). */
export async function getSessionState(sessionId: string): Promise<InterviewSessionState> {
  const { data } = await apiClient.get<InterviewSessionState>(`/sessions/${sessionId}/state`)
  return data
}

/** POST /api/v1/sessions/{id}/start — CREATED → IN_PROGRESS, asks the first question (FR-09/16). */
export async function startSession(sessionId: string): Promise<InterviewSessionState> {
  const { data } = await apiClient.post<InterviewSessionState>(`/sessions/${sessionId}/start`)
  return data
}

/**
 * POST /api/v1/sessions/{id}/turns/{turnId}/answer — submit an answer (FR-17, BR-17 immutable).
 * `requestId` makes the call idempotent: retries with the same id never double-submit.
 */
export async function submitAnswer(
  sessionId: string,
  turnId: string,
  requestId: string,
  answerText: string,
): Promise<InterviewSessionState> {
  const { data } = await apiClient.post<InterviewSessionState>(
    `/sessions/${sessionId}/turns/${turnId}/answer`,
    { requestId, answerText },
  )
  return data
}

/** POST /api/v1/sessions/{id}/turns/{turnId}/skip — skip the current question (BR-12: skipped = 0). */
export async function skipTurn(
  sessionId: string,
  turnId: string,
  requestId: string,
): Promise<InterviewSessionState> {
  const { data } = await apiClient.post<InterviewSessionState>(
    `/sessions/${sessionId}/turns/${turnId}/skip`,
    { requestId },
  )
  return data
}

/** POST /api/v1/sessions/{id}/end — end the session early (FR-14) → COMPLETED. */
export async function endSession(sessionId: string): Promise<EndSessionResult> {
  const { data } = await apiClient.post<EndSessionResult>(`/sessions/${sessionId}/end`)
  return data
}

/** GET /api/v1/sessions/{id}/report — full scores; refused until the session has ended (BR-20). */
export async function getSessionReport(sessionId: string): Promise<SessionReport> {
  const { data } = await apiClient.get<SessionReport>(`/sessions/${sessionId}/report`)
  return data
}

// ─── JD analysis + session creation (Step 4 candidate pages) ─────────────────

export interface JdTermView {
  jdTopicId: string
  rawTerm: string
  topicId: string | null   // null for unsupported terms (FR-38)
  topicCode: string | null
  importance: string
  selected: boolean
}

export interface JdAnalysisView {
  jdId: string
  level: Level
  terms: JdTermView[]
}

export interface SessionSummaryView {
  sessionId: string
  status: SessionStatus
  level: Level
  totalScore: number | null   // null until the session has ended
  createdAt: string
}

/** POST /api/v1/jd/analyze — candidate pastes a JD (FR-06). */
export async function analyzeJd(rawText: string): Promise<JdAnalysisView> {
  const { data } = await apiClient.post<JdAnalysisView>('/jd/analyze', { rawText })
  return data
}

/** POST /api/v1/sessions — create a session from an analysed JD (FR-09). */
export async function createSession(req: {
  jdId: string
  level: Level
  timeLimitSec: number
  selectedTopicIds: string[]
}): Promise<InterviewSessionState> {
  const { data } = await apiClient.post<InterviewSessionState>('/sessions', req)
  return data
}

/** GET /api/v1/sessions — list the candidate's sessions (FR-24 dashboard). */
export async function listSessions(): Promise<SessionSummaryView[]> {
  const { data } = await apiClient.get<SessionSummaryView[]>('/sessions')
  return data
}
