/**
 * Question API client — design §3.3, FR-32.
 *
 * All calls use the shared apiClient (Bearer token interceptor already configured).
 */
import { apiClient } from './authApi'

// ─── Types (mirror backend QuestionResponse / QuestionRequest) ──────────────

export interface QuestionSummaryDto {
  id: string
  topicId: string | null
  language: string
  level: string
  content: string
  status: string
  indexStatus: string
}

export interface KeyPointDto {
  id: string
  seq: number
  content: string
}

export interface QuestionDetailDto {
  id: string
  topicId: string | null
  language: string
  level: string
  content: string
  referenceAnswer: string | null
  status: string
  indexStatus: string
  keyPoints: KeyPointDto[]
}

export interface KeyPointRequest {
  seq: number
  content: string
}

export interface QuestionCreateRequest {
  topicId: string
  language?: string
  level: string
  content: string
  referenceAnswer?: string
  keyPoints?: KeyPointRequest[]
}

export interface QuestionUpdateRequest {
  topicId: string
  language?: string
  level: string
  content: string
  referenceAnswer?: string
  keyPoints?: KeyPointRequest[]
}

export interface PublishResult {
  id: string
  status: string
  indexStatus: string
  indexingTriggered: boolean
}

/** 422 body when publish fails validation (AC-Q-01). */
export interface PublishValidationError {
  status: number
  message: string
  missingFields: string[]
}

// ─── CRUD ────────────────────────────────────────────────────────────────────

/** GET /admin/questions → 200 [QuestionSummaryDto] */
export async function listQuestions(): Promise<QuestionSummaryDto[]> {
  const { data } = await apiClient.get<QuestionSummaryDto[]>('/admin/questions')
  return data
}

/** POST /admin/questions → 201 QuestionDetailDto */
export async function createQuestion(req: QuestionCreateRequest): Promise<QuestionDetailDto> {
  const { data } = await apiClient.post<QuestionDetailDto>('/admin/questions', req)
  return data
}

/** GET /admin/questions/{id} → 200 QuestionDetailDto | 404 */
export async function getQuestion(id: string): Promise<QuestionDetailDto> {
  const { data } = await apiClient.get<QuestionDetailDto>(`/admin/questions/${id}`)
  return data
}

/** PUT /admin/questions/{id} → 200 QuestionDetailDto */
export async function updateQuestion(id: string, req: QuestionUpdateRequest): Promise<QuestionDetailDto> {
  const { data } = await apiClient.put<QuestionDetailDto>(`/admin/questions/${id}`, req)
  return data
}

/** DELETE /admin/questions/{id} → 204 */
export async function deleteQuestion(id: string): Promise<void> {
  await apiClient.delete(`/admin/questions/${id}`)
}

// ─── Lifecycle actions ────────────────────────────────────────────────────────

/** POST /admin/questions/{id}/publish → 200 PublishResult | 422 PublishValidationError */
export async function publishQuestion(id: string): Promise<PublishResult> {
  const { data } = await apiClient.post<PublishResult>(`/admin/questions/${id}/publish`)
  return data
}

/** POST /admin/questions/{id}/archive → 200 QuestionDetailDto */
export async function archiveQuestion(id: string): Promise<QuestionDetailDto> {
  const { data } = await apiClient.post<QuestionDetailDto>(`/admin/questions/${id}/archive`)
  return data
}
