import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listStudyDocuments, type CandidateDocumentDto } from '../../api/candidateDocumentApi'
import '../../styles/candidateDocuments.css'

function formatDate(iso: string): string {
  if (!iso) return '—'
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  })
}

function topicLabel(document: CandidateDocumentDto): string {
  return document.topicName || document.topicCode || 'Chưa phân loại'
}

export default function DocumentsPage() {
  const navigate = useNavigate()
  const [documents, setDocuments] = useState<CandidateDocumentDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [topicFilter, setTopicFilter] = useState('Tất cả')

  useEffect(() => {
    listStudyDocuments()
      .then((data) => setDocuments(data.filter((document) => document.status === 'INDEXED')))
      .catch(() => setError('Không thể tải danh sách tài liệu ôn tập.'))
      .finally(() => setLoading(false))
  }, [])

  const topicOptions = useMemo(() => {
    const labels = Array.from(new Set(documents.map(topicLabel).filter(Boolean)))
    return ['Tất cả', ...labels]
  }, [documents])

  const filteredDocuments = useMemo(() => {
    const keyword = search.trim().toLowerCase()
    return documents.filter((document) => {
      const matchesTopic = topicFilter === 'Tất cả' || topicLabel(document) === topicFilter
      const matchesSearch = !keyword || document.title.toLowerCase().includes(keyword)
      return matchesTopic && matchesSearch
    })
  }, [documents, search, topicFilter])

  return (
    <div className="study-documents-page">
      <header className="study-documents-header">
        <div>
          <span className="study-kicker">THƯ VIỆN HỌC TẬP</span>
          <h1>Tài liệu ôn tập</h1>
          <p>Chọn tài liệu phù hợp để ôn kiến thức trước buổi phỏng vấn.</p>
        </div>
      </header>

      <div className="study-toolbar">
        <label className="study-search">
          <span aria-hidden="true">⌕</span>
          <input
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Tìm theo tiêu đề tài liệu…"
            aria-label="Tìm tài liệu"
          />
        </label>

        <div className="study-filter-pills" aria-label="Lọc theo chủ đề">
          {topicOptions.map((topic) => (
            <button
              key={topic}
              type="button"
              className={'study-filter-pill' + (topicFilter === topic ? ' active' : '')}
              onClick={() => setTopicFilter(topic)}
            >
              {topic}
            </button>
          ))}
        </div>
      </div>

      {loading && <p className="study-state">Đang tải tài liệu…</p>}
      {error && <p className="study-state study-error" role="alert">{error}</p>}

      {!loading && !error && filteredDocuments.length === 0 && (
        <div className="study-empty">
          <h2>Không tìm thấy tài liệu</h2>
          <p>Thử đổi từ khóa hoặc chọn chủ đề khác.</p>
        </div>
      )}

      {!loading && !error && filteredDocuments.length > 0 && (
        <div className="study-document-grid">
          {filteredDocuments.map((document) => (
            <article className="study-document-card" key={document.id}>
              <div>
                <span className="study-topic-badge">{topicLabel(document)}</span>
                <h2>{document.title}</h2>
              </div>
              <div className="study-document-card-footer">
                <span>Cập nhật: {formatDate(document.updatedAt)}</span>
                <button type="button" onClick={() => navigate(`/documents/${document.id}`)}>
                  Đọc tài liệu <span aria-hidden="true">→</span>
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  )
}
