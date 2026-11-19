/**
 * DocumentsPage — Admin CRUD for study materials (FR-33, NFR-26).
 *
 * Features:
 *  - List documents (summary: topic, title, status, dates)
 *  - Create document (+auto-trigger indexing)
 *  - Edit document
 *  - Delete document
 *  - Reindex document (manual trigger)
 *
 * All UI text is Vietnamese (NFR-26).
 */
import React, { useEffect, useState } from 'react'
import {
  createDocument,
  deleteDocument,
  getDocument,
  listDocuments,
  reindexDocument,
  updateDocument,
  type DocumentSummaryDto,
} from '../../api/documentApi'
import { listTopics, type TopicDto } from '../../api/topicApi'
import { adminStyles } from '../../components/shared/adminStyles'
import ConfirmDialog from '../../components/shared/ConfirmDialog'
import { IconEdit, IconRefresh, IconTrash } from '../../components/ui/icons'

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Bản nháp',
  INDEXED: 'Đã đánh chỉ mục',
  INDEX_FAILED: 'Đánh chỉ mục thất bại',
}

function topicCode(topicId: string | null, topics: TopicDto[]): string {
  if (!topicId) return '—'
  return topics.find((t) => t.id === topicId)?.code ?? topicId.slice(0, 8)
}

function formatDate(iso: string): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return d.toLocaleString('vi-VN')
}

function getStatus(err: unknown): number | null {
  if (err && typeof err === 'object' && 'response' in err && err.response && typeof err.response === 'object' && 'status' in err.response) {
    return (err.response as { status: number }).status
  }
  return null
}

export default function DocumentsPage() {
  const [documents, setDocuments] = useState<DocumentSummaryDto[]>([])
  const [topics, setTopics] = useState<TopicDto[]>([])
  const [loading, setLoading] = useState(true)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  // Form modal state
  const [formOpen, setFormOpen] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [topicId, setTopicId] = useState('')
  const [language, setLanguage] = useState('vi')
  const [title, setTitle] = useState('')
  const [contentMd, setContentMd] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // Delete confirmation
  const [deletingId, setDeletingId] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setErrorMsg(null)
    try {
      const [docs, ts] = await Promise.all([listDocuments(), listTopics()])
      setDocuments(docs)
      setTopics(ts)
    } catch {
      setErrorMsg('Không thể tải danh sách tài liệu.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  function openCreate() {
    setEditingId(null)
    setTopicId(topics[0]?.id ?? '')
    setLanguage('vi')
    setTitle('')
    setContentMd('')
    setFormOpen(true)
  }

  function openEdit(doc: DocumentSummaryDto) {
    setEditingId(doc.id)
    setTopicId(doc.topicId ?? '')
    setLanguage(doc.language)
    setTitle(doc.title)
    setContentMd('')
    setFormOpen(true)
    // Fetch full content
    getDocument(doc.id).then((d) => setContentMd(d.contentMd)).catch(() => { /* keep empty */ })
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

    try {
      if (editingId) {
        await updateDocument(editingId, { topicId, language, title, contentMd })
        setSuccessMsg('Đã cập nhật tài liệu. Nhấn "Đánh chỉ mục lại" để cập nhật vector.')
      } else {
        await createDocument({ topicId, language, title, contentMd })
        setSuccessMsg('Đã tạo tài liệu.')
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
        setErrorMsg('Có lỗi xảy ra khi lưu tài liệu.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleReindex(doc: DocumentSummaryDto) {
    setErrorMsg(null)
    setSuccessMsg(null)
    try {
      const result = await reindexDocument(doc.id)
      if (result.status === 'INDEXED') {
        setSuccessMsg('Đã đánh chỉ mục lại thành công.')
      } else {
        setErrorMsg(`Đánh chỉ mục thất bại: ${result.indexError ?? 'lỗi không xác định'}.`)
      }
      await load()
    } catch {
      setErrorMsg('Không thể đánh chỉ mục lại tài liệu.')
    }
  }

  async function confirmDelete() {
    if (!deletingId) return
    setErrorMsg(null)
    setSuccessMsg(null)
    try {
      await deleteDocument(deletingId)
      setSuccessMsg('Đã xóa tài liệu.')
      await load()
    } catch (err) {
      const status = getStatus(err)
      if (status === 409) {
        setErrorMsg('Không thể xóa: tài liệu đã được đánh chỉ mục và đang được tham chiếu.')
      } else {
        setErrorMsg('Không thể xóa tài liệu.')
      }
    } finally {
      setDeletingId(null)
    }
  }

  // ─── Render ─────────────────────────────────────────────────────────────────

  return (
    <div style={adminStyles.page}>
      <div style={adminStyles.pageHeader}>
        <h1 style={adminStyles.pageHeading}>Quản lý tài liệu</h1>
        <button type="button" style={adminStyles.primaryButton} onClick={openCreate} disabled={topics.length === 0}>
          + Thêm tài liệu
        </button>
      </div>

      {errorMsg && !formOpen && <p role="alert" style={adminStyles.errorText}>{errorMsg}</p>}
      {successMsg && !formOpen && <p style={adminStyles.successText}>{successMsg}</p>}

      {topics.length === 0 && !loading && (
        <p style={adminStyles.errorText}>Cần tạo ít nhất một chủ đề trước khi thêm tài liệu.</p>
      )}

      {loading ? (
        <p>Đang tải…</p>
      ) : documents.length === 0 ? (
        <p>Chưa có tài liệu nào.</p>
      ) : (
        <table style={adminStyles.table}>
          <thead>
            <tr>
              <th style={adminStyles.th}>Chủ đề</th>
              <th style={adminStyles.th}>Tiêu đề</th>
              <th style={adminStyles.th}>Trạng thái</th>
              <th style={adminStyles.th}>Cập nhật</th>
              <th style={adminStyles.th}>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {documents.map((doc) => (
              <tr key={doc.id}>
                <td style={adminStyles.td}>{topicCode(doc.topicId, topics)}</td>
                <td style={{ ...adminStyles.td, maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {doc.title}
                </td>
                <td style={adminStyles.td}>{STATUS_LABELS[doc.status] ?? doc.status}</td>
                <td style={adminStyles.td}>{formatDate(doc.updatedAt)}</td>
                <td style={adminStyles.td}>
                  <button style={adminStyles.smallButton} onClick={() => openEdit(doc)}>
                    <IconEdit style={{ marginRight: 4 }} /> Sửa
                  </button>
                  <button type="button" className="icon-action" title="Đánh chỉ mục lại" aria-label="Đánh chỉ mục lại" onClick={() => handleReindex(doc)}>
                    <IconRefresh size={16} />
                  </button>
                  <button type="button" className="icon-action danger" title="Xóa tài liệu" aria-label="Xóa" onClick={() => setDeletingId(doc.id)}>
                    <IconTrash size={16} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* Create / Edit modal */}
      {formOpen && (
        <div style={adminStyles.modalOverlay} role="dialog" aria-modal="true" aria-label={editingId ? 'Sửa tài liệu' : 'Thêm tài liệu'}>
          <form style={adminStyles.modalCard} onSubmit={handleSubmit}>
            <h2 style={adminStyles.modalTitle}>{editingId ? 'Sửa tài liệu' : 'Thêm tài liệu'}</h2>

            {errorMsg && <p role="alert" style={adminStyles.errorText}>{errorMsg}</p>}

            <div style={adminStyles.field}>
              <label htmlFor="doc-topic" style={adminStyles.label}>Chủ đề</label>
              <select id="doc-topic" style={adminStyles.select} value={topicId} onChange={(e) => setTopicId(e.target.value)} required>
                <option value="">— Chọn chủ đề —</option>
                {topics.map((t) => (
                  <option key={t.id} value={t.id}>{t.code}</option>
                ))}
              </select>
            </div>

            <div style={{ display: 'flex', gap: '1rem' }}>
              <div style={{ flex: 2, ...adminStyles.field }}>
                <label htmlFor="doc-title" style={adminStyles.label}>Tiêu đề</label>
                <input id="doc-title" style={adminStyles.input} value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={400} />
              </div>
              <div style={{ flex: 1, ...adminStyles.field }}>
                <label htmlFor="doc-language" style={adminStyles.label}>Ngôn ngữ</label>
                <select id="doc-language" style={adminStyles.select} value={language} onChange={(e) => setLanguage(e.target.value)}>
                  <option value="vi">vi</option>
                  <option value="en">en</option>
                </select>
              </div>
            </div>

            <div style={adminStyles.field}>
              <label htmlFor="doc-content" style={adminStyles.label}>Nội dung (Markdown)</label>
              <textarea
                id="doc-content"
                style={{ ...adminStyles.textarea, minHeight: 200 }}
                value={contentMd}
                onChange={(e) => setContentMd(e.target.value)}
                required
                placeholder="Nội dung tài liệu bằng Markdown…"
              />
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
          title="Xóa tài liệu"
          message="Bạn có chắc muốn xóa tài liệu này? Hành động này không thể hoàn tác."
          confirmLabel="Xóa"
          onConfirm={confirmDelete}
          onCancel={() => setDeletingId(null)}
        />
      )}
    </div>
  )
}
