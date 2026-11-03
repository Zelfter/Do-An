/**
 * QuestionsPage — Admin CRUD for the question bank (FR-32, NFR-26).
 *
 * Features:
 *  - List questions (summary: topic, level, content preview, status, index status)
 *  - Create question (DRAFT by default)
 *  - Edit question (full update incl. key points)
 *  - Delete question
 *  - Publish question (200 | 422 missingFields)
 *  - Archive question
 *
 * All UI text is Vietnamese (NFR-26).
 */
import React, { useEffect, useState } from 'react'
import {
  archiveQuestion,
  createQuestion,
  deleteQuestion,
  getQuestion,
  listQuestions,
  publishQuestion,
  updateQuestion,
  type KeyPointRequest,
  type QuestionDetailDto,
  type QuestionSummaryDto,
} from '../../api/questionApi'
import { listTopics, type TopicDto } from '../../api/topicApi'
import { adminStyles } from '../../components/shared/adminStyles'
import ConfirmDialog from '../../components/shared/ConfirmDialog'

const LEVELS = ['JUNIOR', 'MID', 'SENIOR']
const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Bản nháp',
  PUBLISHED: 'Đã xuất bản',
  ARCHIVED: 'Đã lưu trữ',
}
// NFR-26: Vietnamese labels for enum values displayed to users.
const LEVEL_LABELS: Record<string, string> = {
  JUNIOR: 'Sơ cấp',
  MID: 'Trung cấp',
  SENIOR: 'Cao cấp',
}
const INDEX_STATUS_LABELS: Record<string, string> = {
  NOT_INDEXED: 'Chưa đánh chỉ mục',
  INDEXING: 'Đang đánh chỉ mục',
  INDEXED: 'Đã đánh chỉ mục',
  INDEX_FAILED: 'Đánh chỉ mục thất bại',
}

function topicCode(topicId: string | null, topics: TopicDto[]): string {
  if (!topicId) return '—'
  return topics.find((t) => t.id === topicId)?.code ?? topicId.slice(0, 8)
}

function getStatus(err: unknown): number | null {
  if (err && typeof err === 'object' && 'response' in err && err.response && typeof err.response === 'object' && 'status' in err.response) {
    return (err.response as { status: number }).status
  }
  return null
}

export default function QuestionsPage() {
  const [questions, setQuestions] = useState<QuestionSummaryDto[]>([])
  const [topics, setTopics] = useState<TopicDto[]>([])
  const [loading, setLoading] = useState(true)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  // Form modal state
  const [formOpen, setFormOpen] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [topicId, setTopicId] = useState('')
  const [level, setLevel] = useState('JUNIOR')
  const [language, setLanguage] = useState('vi')
  const [content, setContent] = useState('')
  const [referenceAnswer, setReferenceAnswer] = useState('')
  const [keyPointsText, setKeyPointsText] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // Delete confirmation
  const [deletingId, setDeletingId] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setErrorMsg(null)
    try {
      const [qs, ts] = await Promise.all([listQuestions(), listTopics()])
      setQuestions(qs)
      setTopics(ts)
    } catch {
      setErrorMsg('Không thể tải danh sách câu hỏi.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  function openCreate() {
    setEditingId(null)
    setTopicId(topics[0]?.id ?? '')
    setLevel('JUNIOR')
    setLanguage('vi')
    setContent('')
    setReferenceAnswer('')
    setKeyPointsText('')
    setFormOpen(true)
  }

  function openEdit(q: QuestionSummaryDto) {
    setEditingId(q.id)
    setTopicId(q.topicId ?? '')
    setLevel(q.level)
    setLanguage(q.language)
    setContent(q.content)
    setReferenceAnswer('')
    setKeyPointsText('')
    setFormOpen(true)
    // Fetch full detail to populate reference answer + key points
    getQuestion(q.id).then((detail: QuestionDetailDto) => {
      setReferenceAnswer(detail.referenceAnswer ?? '')
      setKeyPointsText(detail.keyPoints.map((kp) => kp.content).join('\n'))
    }).catch(() => { /* keep defaults */ })
  }

  function closeForm() {
    setFormOpen(false)
    setSubmitting(false)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setErrorMsg(null)
    setSuccessMsg(null)

    const keyPoints: KeyPointRequest[] = keyPointsText
      .split('\n')
      .map((t) => t.trim())
      .filter(Boolean)
      .map((c, i) => ({ seq: i + 1, content: c }))

    try {
      if (editingId) {
        await updateQuestion(editingId, {
          topicId,
          language,
          level,
          content,
          referenceAnswer: referenceAnswer || undefined,
          keyPoints,
        })
        setSuccessMsg('Đã cập nhật câu hỏi.')
      } else {
        await createQuestion({
          topicId,
          language,
          level,
          content,
          referenceAnswer: referenceAnswer || undefined,
          keyPoints,
        })
        setSuccessMsg('Đã tạo câu hỏi (bản nháp).')
      }
      closeForm()
      await load()
    } catch (err) {
      const status = getStatus(err)
      if (status === 404) {
        setErrorMsg('Chủ đề không tồn tại.')
      } else if (status === 400) {
        setErrorMsg('Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.')
      } else {
        setErrorMsg('Có lỗi xảy ra khi lưu câu hỏi.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handlePublish(q: QuestionSummaryDto) {
    setErrorMsg(null)
    setSuccessMsg(null)
    try {
      await publishQuestion(q.id)
      setSuccessMsg('Đã xuất bản câu hỏi.')
      await load()
    } catch (err: unknown) {
      const status = getStatus(err)
      if (status === 422) {
        const body = (err as { response?: { data?: { missingFields?: string[] } } }).response?.data
        const missing = body?.missingFields?.join(', ') ?? 'các trường bắt buộc'
        setErrorMsg(`Không thể xuất bản: thiếu ${missing}.`)
      } else if (status === 409) {
        setErrorMsg('Không thể xuất bản câu hỏi.')
      } else {
        setErrorMsg('Có lỗi xảy ra khi xuất bản câu hỏi.')
      }
    }
  }

  async function handleArchive(q: QuestionSummaryDto) {
    setErrorMsg(null)
    setSuccessMsg(null)
    try {
      await archiveQuestion(q.id)
      setSuccessMsg('Đã lưu trữ câu hỏi.')
      await load()
    } catch {
      setErrorMsg('Không thể lưu trữ câu hỏi.')
    }
  }

  async function confirmDelete() {
    if (!deletingId) return
    setErrorMsg(null)
    setSuccessMsg(null)
    try {
      await deleteQuestion(deletingId)
      setSuccessMsg('Đã xóa câu hỏi.')
      await load()
    } catch (err) {
      const status = getStatus(err)
      if (status === 409) {
        setErrorMsg('Không thể xóa: câu hỏi đang được sử dụng trong phiên phỏng vấn.')
      } else {
        setErrorMsg('Không thể xóa câu hỏi.')
      }
    } finally {
      setDeletingId(null)
    }
  }

  // ─── Render ─────────────────────────────────────────────────────────────────

  return (
    <div style={adminStyles.page}>
      <div style={adminStyles.pageHeader}>
        <h1 style={adminStyles.pageHeading}>Ngân hàng câu hỏi</h1>
        <button type="button" style={adminStyles.primaryButton} onClick={openCreate} disabled={topics.length === 0}>
          + Thêm câu hỏi
        </button>
      </div>

      {errorMsg && !formOpen && <p role="alert" style={adminStyles.errorText}>{errorMsg}</p>}
      {successMsg && !formOpen && <p style={adminStyles.successText}>{successMsg}</p>}

      {topics.length === 0 && !loading && (
        <p style={adminStyles.errorText}>Cần tạo ít nhất một chủ đề trước khi thêm câu hỏi.</p>
      )}

      {loading ? (
        <p>Đang tải…</p>
      ) : questions.length === 0 ? (
        <p>Chưa có câu hỏi nào.</p>
      ) : (
        <table style={adminStyles.table}>
          <thead>
            <tr>
              <th style={adminStyles.th}>Chủ đề</th>
              <th style={adminStyles.th}>Cấp độ</th>
              <th style={adminStyles.th}>Nội dung</th>
              <th style={adminStyles.th}>Trạng thái</th>
              <th style={adminStyles.th}>Chỉ mục</th>
              <th style={adminStyles.th}>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {questions.map((q) => (
              <tr key={q.id}>
                <td style={adminStyles.td}>{topicCode(q.topicId, topics)}</td>
                <td style={adminStyles.td}>{LEVEL_LABELS[q.level] ?? q.level}</td>
                <td style={{ ...adminStyles.td, maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {q.content}
                </td>
                <td style={adminStyles.td}>{STATUS_LABELS[q.status] ?? q.status}</td>
                <td style={adminStyles.td}>{INDEX_STATUS_LABELS[q.indexStatus] ?? q.indexStatus}</td>
                <td style={adminStyles.td}>
                  <button style={adminStyles.smallButton} onClick={() => openEdit(q)}>Sửa</button>
                  {q.status !== 'PUBLISHED' && (
                    <button style={adminStyles.smallButton} onClick={() => handlePublish(q)}>Xuất bản</button>
                  )}
                  {q.status !== 'ARCHIVED' && (
                    <button style={adminStyles.smallButton} onClick={() => handleArchive(q)}>Lưu trữ</button>
                  )}
                  <button style={{ ...adminStyles.smallButton, color: '#ff4d4f' }} onClick={() => setDeletingId(q.id)}>Xóa</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* Create / Edit modal */}
      {formOpen && (
        <div style={adminStyles.modalOverlay} role="dialog" aria-modal="true" aria-label={editingId ? 'Sửa câu hỏi' : 'Thêm câu hỏi'}>
          <form style={adminStyles.modalCard} onSubmit={handleSubmit}>
            <h2 style={adminStyles.modalTitle}>{editingId ? 'Sửa câu hỏi' : 'Thêm câu hỏi'}</h2>

            {errorMsg && <p role="alert" style={adminStyles.errorText}>{errorMsg}</p>}

            <div style={adminStyles.field}>
              <label htmlFor="q-topic" style={adminStyles.label}>Chủ đề</label>
              <select id="q-topic" style={adminStyles.select} value={topicId} onChange={(e) => setTopicId(e.target.value)} required>
                <option value="">— Chọn chủ đề —</option>
                {topics.map((t) => (
                  <option key={t.id} value={t.id}>{t.code}</option>
                ))}
              </select>
            </div>

            <div style={{ display: 'flex', gap: '1rem' }}>
              <div style={{ flex: 1, ...adminStyles.field }}>
                <label htmlFor="q-level" style={adminStyles.label}>Cấp độ</label>
                <select id="q-level" style={adminStyles.select} value={level} onChange={(e) => setLevel(e.target.value)}>
                  {LEVELS.map((l) => <option key={l} value={l}>{LEVEL_LABELS[l] ?? l}</option>)}
                </select>
              </div>
              <div style={{ flex: 1, ...adminStyles.field }}>
                <label htmlFor="q-language" style={adminStyles.label}>Ngôn ngữ</label>
                <select id="q-language" style={adminStyles.select} value={language} onChange={(e) => setLanguage(e.target.value)}>
                  <option value="vi">vi</option>
                  <option value="en">en</option>
                </select>
              </div>
            </div>

            <div style={adminStyles.field}>
              <label htmlFor="q-content" style={adminStyles.label}>Nội dung câu hỏi</label>
              <textarea id="q-content" style={adminStyles.textarea} value={content} onChange={(e) => setContent(e.target.value)} required />
            </div>

            <div style={adminStyles.field}>
              <label htmlFor="q-reference" style={adminStyles.label}>Câu trả lời tham khảo (bắt buộc khi xuất bản)</label>
              <textarea id="q-reference" style={adminStyles.textarea} value={referenceAnswer} onChange={(e) => setReferenceAnswer(e.target.value)} />
            </div>

            <div style={adminStyles.field}>
              <label htmlFor="q-keypoints" style={adminStyles.label}>Điểm chính (mỗi dòng một điểm, bắt buộc ≥1 khi xuất bản)</label>
              <textarea id="q-keypoints" style={adminStyles.textarea} value={keyPointsText} onChange={(e) => setKeyPointsText(e.target.value)} />
            </div>

            <div style={adminStyles.modalActions}>
              <button type="button" style={adminStyles.secondaryButton} onClick={closeForm}>Hủy</button>
              <button type="submit" style={adminStyles.primaryButton} disabled={submitting}>
                {submitting ? 'Đang lưu…' : 'Lưu'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Delete confirmation */}
      {deletingId && (
        <ConfirmDialog
          title="Xóa câu hỏi"
          message="Bạn có chắc muốn xóa câu hỏi này? Hành động này không thể hoàn tác."
          confirmLabel="Xóa"
          onConfirm={confirmDelete}
          onCancel={() => setDeletingId(null)}
        />
      )}
    </div>
  )
}
