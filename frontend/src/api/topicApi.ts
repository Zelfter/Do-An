/**
 * Topic API client — design §3.2, FR-37.
 *
 * All calls use the shared apiClient (Bearer token interceptor already configured).
 * Frontend NEVER calls the Python AI service directly.
 */
import { apiClient } from './authApi'

// ─── Types (mirror backend TopicResponse / TopicRequest) ─────────────────────

export interface TranslationDto {
  language: string
  name: string
  description: string | null
}

export interface AliasDto {
  id: string
  alias: string
}

export interface TopicDto {
  id: string
  code: string
  parentId: string | null
  isActive: boolean
  translations: TranslationDto[]
  aliases: AliasDto[]
}

export interface TopicCreateRequest {
  code: string
  parentId?: string | null
  translations: TranslationDto[]
  aliases?: string[]
}

export interface TopicUpdateRequest {
  parentId?: string | null
  translations: TranslationDto[]
  aliases?: string[]
}

export interface TopicPatchRequest {
  parentId?: string | null
  isActive?: boolean
  translations?: TranslationDto[]
  aliases?: string[]
}

// ─── CRUD ────────────────────────────────────────────────────────────────────

/** GET /admin/topics → 200 [TopicDto] */
export async function listTopics(): Promise<TopicDto[]> {
  const { data } = await apiClient.get<TopicDto[]>('/admin/topics')
  return data
}

/** POST /admin/topics → 201 TopicDto | 409 (code duplicate) */
export async function createTopic(req: TopicCreateRequest): Promise<TopicDto> {
  const { data } = await apiClient.post<TopicDto>('/admin/topics', req)
  return data
}

/** GET /admin/topics/{id} → 200 TopicDto | 404 */
export async function getTopic(id: string): Promise<TopicDto> {
  const { data } = await apiClient.get<TopicDto>(`/admin/topics/${id}`)
  return data
}

/** PUT /admin/topics/{id} → 200 TopicDto */
export async function updateTopic(id: string, req: TopicUpdateRequest): Promise<TopicDto> {
  const { data } = await apiClient.put<TopicDto>(`/admin/topics/${id}`, req)
  return data
}

/** PATCH /admin/topics/{id} → 200 TopicDto (partial update incl. isActive) */
export async function patchTopic(id: string, req: TopicPatchRequest): Promise<TopicDto> {
  const { data } = await apiClient.patch<TopicDto>(`/admin/topics/${id}`, req)
  return data
}

/** DELETE /admin/topics/{id} → 204 | 409 (has linked questions/docs) */
export async function deleteTopic(id: string): Promise<void> {
  await apiClient.delete(`/admin/topics/${id}`)
}

// ─── Aliases ─────────────────────────────────────────────────────────────────

/** GET /admin/topics/{id}/aliases → 200 [AliasDto] */
export async function listAliases(topicId: string): Promise<AliasDto[]> {
  const { data } = await apiClient.get<AliasDto[]>(`/admin/topics/${topicId}/aliases`)
  return data
}

/** POST /admin/topics/{id}/aliases → 201 AliasDto */
export async function addAlias(topicId: string, alias: string): Promise<AliasDto> {
  const { data } = await apiClient.post<AliasDto>(`/admin/topics/${topicId}/aliases`, { alias })
  return data
}

/** DELETE /admin/topics/{id}/aliases/{aliasId} → 204 */
export async function deleteAlias(topicId: string, aliasId: string): Promise<void> {
  await apiClient.delete(`/admin/topics/${topicId}/aliases/${aliasId}`)
}
