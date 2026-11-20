import { apiClient } from './authApi'

export interface CandidateDocumentDto {
  id: string
  topicId: string | null
  topicCode: string | null
  topicName: string | null
  language: string
  title: string
  content: string | null
  contentMd: string | null
  status: string
  createdAt: string
  updatedAt: string
  fileUrl: string | null
}

/** GET /api/v1/documents — only INDEXED documents are exposed to candidates. */
export async function listStudyDocuments(topic?: string, keyword?: string): Promise<CandidateDocumentDto[]> {
  const { data } = await apiClient.get<CandidateDocumentDto[]>('/documents', {
    params: {
      topic: topic || undefined,
      keyword: keyword || undefined,
    },
  })
  return data
}

/** GET /api/v1/documents/{id} — full indexed document detail. */
export async function getStudyDocument(id: string): Promise<CandidateDocumentDto> {
  const { data } = await apiClient.get<CandidateDocumentDto>(`/documents/${id}`)
  return data
}
