/**
 * Document (study material) API client — design §3.4, FR-33.
 *
 * All calls use the shared apiClient (Bearer token interceptor already configured).
 */
import { apiClient } from './authApi'

// ─── Types (mirror backend DocumentResponse / DocumentRequest) ──────────────

export interface DocumentSummaryDto {
  id: string
  topicId: string | null
  language: string
  title: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface DocumentDto {
  id: string
  topicId: string | null
  language: string
  title: string
  contentMd: string
  status: string
  indexError: string | null
  createdAt: string
  updatedAt: string
}

export interface DocumentCreateRequest {
  topicId: string
  language?: string
  title: string
  contentMd: string
}

export interface DocumentUpdateRequest {
  topicId?: string
  language?: string
  title?: string
  contentMd?: string
}

export interface ReindexResult {
  documentId: string
  status: string
  indexError: string | null
}

// ─── CRUD ────────────────────────────────────────────────────────────────────

/** GET /admin/documents → 200 [DocumentSummaryDto] */
export async function listDocuments(): Promise<DocumentSummaryDto[]> {
  const { data } = await apiClient.get<DocumentSummaryDto[]>('/admin/documents')
  return data
}

/** POST /admin/documents → 201 DocumentDto (+auto-trigger indexing) */
export async function createDocument(req: DocumentCreateRequest): Promise<DocumentDto> {
  const { data } = await apiClient.post<DocumentDto>('/admin/documents', req)
  return data
}

/** GET /admin/documents/{id} → 200 DocumentDto | 404 */
export async function getDocument(id: string): Promise<DocumentDto> {
  const { data } = await apiClient.get<DocumentDto>(`/admin/documents/${id}`)
  return data
}

/** PUT /admin/documents/{id} → 200 DocumentDto */
export async function updateDocument(id: string, req: DocumentUpdateRequest): Promise<DocumentDto> {
  const { data } = await apiClient.put<DocumentDto>(`/admin/documents/${id}`, req)
  return data
}

/** DELETE /admin/documents/{id} → 204 */
export async function deleteDocument(id: string): Promise<void> {
  await apiClient.delete(`/admin/documents/${id}`)
}

// ─── Lifecycle actions ────────────────────────────────────────────────────────

/** POST /admin/documents/{id}/reindex → 202 ReindexResult */
export async function reindexDocument(id: string): Promise<ReindexResult> {
  const { data } = await apiClient.post<ReindexResult>(`/admin/documents/${id}/reindex`)
  return data
}
