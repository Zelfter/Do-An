/**
 * TopicsPage — Admin CRUD for topics + aliases (FR-37, NFR-26).
 *
 * Features:
 *  - List topics with vi name + active status
 *  - Create topic (code immutable, vi translation, aliases)
 *  - Edit topic (vi translation, aliases, parent)
 *  - Toggle active (PATCH isActive)
 *  - Delete topic (204 | 409 linked)
 *  - Manage aliases (add / delete)
 *
 * All UI text is Vietnamese (NFR-26).
 */
import React, { useEffect, useState } from 'react'
import {
  addAlias,
  createTopic,
  deleteAlias,
  deleteTopic,
  listAliases,
  listTopics,
  patchTopic,
  updateTopic,
  type AliasDto,
  type TopicDto,
  type TranslationDto,
} from '../../api/topicApi'
import { adminStyles } from '../../components/shared/adminStyles'
import ConfirmDialog from '../../components/shared/ConfirmDialog'
import { IconEdit, IconTag, IconTrash } from '../../components/ui/icons'

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Extract the display name from the 'vi' translation (fallback to any). */
function displayName(topic: TopicDto): string {
  const vi = topic.translations.find((t) => t.language === 'vi')
  if (vi) return vi.name
  return topic.translations[0]?.name ?? topic.code
}

function getStatus(err: unknown): number | null {
  if (err && typeof err === 'object' && 'response' in err && err.response && typeof err.response === 'object' && 'status' in err.response) {
    return (err.response as { status: number }).status
  }
  return null
}

export default function TopicsPage() {
  const [topics, setTopics] = useState<TopicDto[]>([])
  const [loading, setLoading] = useState(true)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  // Create/Edit modal state
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<TopicDto | null>(null)
  const [code, setCode] = useState('')
  const [parentId, setParentId] = useState('')
  const [viName, setViName] = useState('')
  const [viDescription, setViDescription] = useState('')
  const [aliasesText, setAliasesText] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // Delete confirmation state
  const [deletingId, setDeletingId] = useState<string | null>(null)

  // Alias management modal state
  const [aliasTopic, setAliasTopic] = useState<TopicDto | null>(null)
  const [aliases, setAliases] = useState<AliasDto[]>([])
  const [newAlias, setNewAlias] = useState('')
  const [aliasLoading, setAliasLoading] = useState(false)

  async function loadTopics() {
    setLoading(true)
    setErrorMsg(null)
    try {
      const data = await listTopics()
      setTopics(data)
    } catch (err) {
      setErrorMsg('Không thể tải danh sách chủ đề.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTopics()
  }, [])

  function openCreate() {
    setEditing(null)
    setCode('')
    setParentId('')
    setViName('')
    setViDescription('')
    setAliasesText('')
    setFormOpen(true)
  }

  function openEdit(topic: TopicDto) {
    setEditing(topic)
    setCode(topic.code)
    setParentId(topic.parentId ?? '')
    const vi = topic.translations.find((t) => t.language === 'vi')
    setViName(vi?.name ?? '')
    setViDescription(vi?.description ?? '')
    setAliasesText(topic.aliases.map((a) => a.alias).join('\n'))
    setFormOpen(true)
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

    const translation: TranslationDto = { language: 'vi', name: viName, description: viDescription || null }
    const aliasList = aliasesText.split('\n').map((a) => a.trim()).filter(Boolean)

    try {
      if (editing) {
        await updateTopic(editing.id, {
          parentId: parentId || null,
          translations: [translation],
          aliases: aliasList,
        })
        setSuccessMsg('Đã cập nhật chủ đề.')
      } else {
        await createTopic({
          code,
          parentId: parentId || null,
          translations: [translation],
          aliases: aliasList,
        })
        setSuccessMsg('Đã tạo chủ đề mới.')
      }
      closeForm()
      await loadTopics()
    } catch (err) {
      const status = getStatus(err)
      if (status === 409) {
        setErrorMsg('Mã chủ đề đã tồn tại.')
      } else if (status === 400) {
        setErrorMsg('Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.')
      } else {
        setErrorMsg('Có lỗi xảy ra khi lưu chủ đề.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleToggleActive(topic: TopicDto) {
    setErrorMsg(null)
    setSuccessMsg(null)
    try {
      await patchTopic(topic.id, { isActive: !topic.isActive })
      setSuccessMsg(topic.isActive ? 'Đã tắt chủ đề.' : 'Đã bật chủ đề.')
      await loadTopics()
    } catch (err) {
      setErrorMsg('Không thể cập nhật trạng thái chủ đề.')
    }
  }

  async function confirmDelete() {
    if (!deletingId) return
    setErrorMsg(null)
    setSuccessMsg(null)
    try {
      await deleteTopic(deletingId)
      setSuccessMsg('Đã xóa chủ đề.')
      await loadTopics()
    } catch (err) {
      const status = getStatus(err)
      if (status === 409) {
        setErrorMsg('Không thể xóa: chủ đề đang có câu hỏi hoặc tài liệu liên kết.')
      } else {
        setErrorMsg('Không thể xóa chủ đề.')
      }
    } finally {
      setDeletingId(null)
    }
  }

  // ─── Alias management ───────────────────────────────────────────────────────

  async function openAliases(topic: TopicDto) {
    setAliasTopic(topic)
    setNewAlias('')
    setAliasLoading(true)
    try {
      const data = await listAliases(topic.id)
      setAliases(data)
    } catch {
      setAliases(topic.aliases)
    } finally {
      setAliasLoading(false)
    }
  }

  async function handleAddAlias() {
    if (!aliasTopic || !newAlias.trim()) return
    setAliasLoading(true)
    try {
      const created = await addAlias(aliasTopic.id, newAlias.trim())
      setAliases((prev) => [...prev, created])
      setNewAlias('')
    } catch (err) {
      const status = getStatus(err)
      if (status === 409) {
        setErrorMsg('Alias đã tồn tại cho chủ đề này.')
      } else {
        setErrorMsg('Không thể thêm alias.')
      }
    } finally {
      setAliasLoading(false)
    }
  }

  async function handleDeleteAlias(aliasId: string) {
    if (!aliasTopic) return
    setAliasLoading(true)
    try {
      await deleteAlias(aliasTopic.id, aliasId)
      setAliases((prev) => prev.filter((a) => a.id !== aliasId))
    } catch {
      setErrorMsg('Không thể xóa alias.')
    } finally {
      setAliasLoading(false)
    }
  }

  // ─── Render ─────────────────────────────────────────────────────────────────

  return (
    <div style={adminStyles.page}>
      <div style={adminStyles.pageHeader}>
        <h1 style={adminStyles.pageHeading}>Quản lý chủ đề</h1>
        <button type="button" style={adminStyles.primaryButton} onClick={openCreate}>
          + Thêm chủ đề
        </button>
      </div>

      {errorMsg && !formOpen && <p role="alert" style={adminStyles.errorText}>{errorMsg}</p>}
      {successMsg && !formOpen && <p style={adminStyles.successText}>{successMsg}</p>}

      {loading ? (
        <div className="admin-card"><p className="muted" style={{ padding: 24 }}>Đang tải…</p></div>
      ) : topics.length === 0 ? (
        <div className="admin-card admin-empty">
          <h3>Chưa có chủ đề nào</h3>
          <p>Thêm chủ đề đầu tiên để bắt đầu xây dựng ngân hàng kiến thức.</p>
          <button type="button" className="btn btn-primary" onClick={openCreate}>+ Thêm chủ đề</button>
          <span style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0 0 0 0)' }}>Chưa có chủ đề nào.</span>
        </div>
      ) : (
        <div className="admin-card">
          <table className="admin-table">
            <thead><tr><th>Mã</th><th>Tên (vi)</th><th>Trạng thái</th><th>Alias</th><th>Thao tác</th></tr></thead>
            <tbody>
              {topics.map((topic) => (
                <tr key={topic.id}>
                  <td><span className="code-badge">{topic.code}</span></td>
                  <td style={{ fontWeight: 600, color: '#0f172a' }}>{displayName(topic)}</td>
                  <td><span className={'badge ' + (topic.isActive ? 'badge-success' : 'badge-danger')}>{topic.isActive ? 'Hoạt động' : 'Tắt'}</span></td>
                  <td><span className="muted">{topic.aliases.length} alias</span></td>
                  <td>
                    <button type="button" className="icon-action" title="Sửa chủ đề" aria-label="Sửa chủ đề" onClick={() => openEdit(topic)}><IconEdit size={16} /></button>
                    <button type="button" className={'toggle-switch ' + (topic.isActive ? 'active' : '')} title={topic.isActive ? 'Tắt chủ đề' : 'Bật chủ đề'} aria-label={topic.isActive ? 'Tắt chủ đề' : 'Bật chủ đề'} onClick={() => handleToggleActive(topic)}><span /></button>
                    <button type="button" className="icon-action" title="Quản lý alias" aria-label="Quản lý alias" onClick={() => openAliases(topic)} style={{ marginLeft: 6 }}><IconTag size={16} /></button>
                    <button type="button" className="icon-action danger" title="Xóa chủ đề" aria-label="Xóa" onClick={() => setDeletingId(topic.id)}><IconTrash size={16} /></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Create / Edit modal */}
      {formOpen && (
        <div style={adminStyles.modalOverlay} role="dialog" aria-modal="true" aria-label={editing ? 'Sửa chủ đề' : 'Thêm chủ đề'}>
          <form style={adminStyles.modalCard} onSubmit={handleSubmit}>
            <h2 style={adminStyles.modalTitle}>{editing ? 'Sửa chủ đề' : 'Thêm chủ đề'}</h2>

            {errorMsg && <p role="alert" style={adminStyles.errorText}>{errorMsg}</p>}

            <div style={adminStyles.field}>
              <label htmlFor="topic-code" style={adminStyles.label}>Mã (snake_case, không đổi được sau khi tạo)</label>
              <input
                id="topic-code"
                style={adminStyles.input}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                disabled={!!editing}
                required
                placeholder="vd: java_core"
              />
            </div>

            <div style={adminStyles.field}>
              <label htmlFor="topic-parent" style={adminStyles.label}>Chủ đề cha (tùy chọn)</label>
              <select
                id="topic-parent"
                style={adminStyles.select}
                value={parentId}
                onChange={(e) => setParentId(e.target.value)}
              >
                <option value="">— Không có —</option>
                {topics.filter((t) => t.id !== editing?.id).map((t) => (
                  <option key={t.id} value={t.id}>{t.code} — {displayName(t)}</option>
                ))}
              </select>
            </div>

            <div style={adminStyles.field}>
              <label htmlFor="topic-name" style={adminStyles.label}>Tên (tiếng Việt)</label>
              <input
                id="topic-name"
                style={adminStyles.input}
                value={viName}
                onChange={(e) => setViName(e.target.value)}
                required
              />
            </div>

            <div style={adminStyles.field}>
              <label htmlFor="topic-description" style={adminStyles.label}>Mô tả (tiếng Việt, tùy chọn)</label>
              <textarea
                id="topic-description"
                style={adminStyles.textarea}
                value={viDescription}
                onChange={(e) => setViDescription(e.target.value)}
              />
            </div>

            <div style={adminStyles.field}>
              <label htmlFor="topic-aliases" style={adminStyles.label}>Alias (mỗi dòng một alias, tùy chọn)</label>
              <textarea
                id="topic-aliases"
                style={adminStyles.textarea}
                value={aliasesText}
                onChange={(e) => setAliasesText(e.target.value)}
                placeholder="vd:&#10;Java cơ bản&#10;core java"
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
          title="Xóa chủ đề"
          message="Bạn có chắc muốn xóa chủ đề này? Hành động này không thể hoàn tác."
          confirmLabel="Xóa"
          onConfirm={confirmDelete}
          onCancel={() => setDeletingId(null)}
        />
      )}

      {/* Alias management modal */}
      {aliasTopic && (
        <div style={adminStyles.modalOverlay} role="dialog" aria-modal="true" aria-label="Quản lý alias">
          <div style={adminStyles.modalCard}>
            <h2 style={adminStyles.modalTitle}>Quản lý alias — {displayName(aliasTopic)}</h2>

            {errorMsg && <p role="alert" style={adminStyles.errorText}>{errorMsg}</p>}

            {aliasLoading && <p>Đang tải…</p>}
            {!aliasLoading && aliases.length === 0 && <p style={{ color: '#888' }}>Chưa có alias nào.</p>}

            <ul style={{ listStyle: 'none', padding: 0, margin: '0 0 1rem' }}>
              {aliases.map((a) => (
                <li key={a.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.4rem 0', borderBottom: '1px solid #f0f0f0' }}>
                  <span>{a.alias}</span>
                  <button style={{ ...adminStyles.smallButton, color: '#ff4d4f' }} onClick={() => handleDeleteAlias(a.id)}>
                    Xóa
                  </button>
                </li>
              ))}
            </ul>

            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <input
                style={adminStyles.input}
                placeholder="Alias mới"
                value={newAlias}
                onChange={(e) => setNewAlias(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleAddAlias() } }}
              />
              <button type="button" style={adminStyles.primaryButton} onClick={handleAddAlias}>Thêm</button>
            </div>

            <div style={adminStyles.modalActions}>
              <button type="button" style={adminStyles.secondaryButton} onClick={() => setAliasTopic(null)}>Đóng</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
