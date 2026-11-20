import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getStudyDocument, type CandidateDocumentDto } from '../../api/candidateDocumentApi'
import '../../styles/candidateDocuments.css'

function formatDate(iso: string): string {
  if (!iso) return '—'
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

function topicLabel(document: CandidateDocumentDto): string {
  return document.topicName || document.topicCode || 'Chưa phân loại'
}

function isHtml(value: string): boolean {
  return /<(!doctype|html|head|body|section|article|h[1-6]|p|div|ul|ol|table|pre|code)[\s>]/i.test(value)
}

function isFileUrl(value: string): boolean {
  return /^(https?:\/\/|\/).*\.(pdf|doc|docx)(\?.*)?$/i.test(value)
}

function renderInline(value: string): React.ReactNode[] {
  const parts = value.split(/(`[^`]+`|\*\*[^*]+\*\*)/g)
  return parts.map((part, index) => {
    if (part.startsWith('`') && part.endsWith('`')) {
      return <code key={index}>{part.slice(1, -1)}</code>
    }
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={index}>{part.slice(2, -2)}</strong>
    }
    return <span key={index}>{part}</span>
  })
}

function MarkdownContent({ markdown }: { markdown: string }) {
  const blocks = useMemo(() => {
    const nodes: React.ReactNode[] = []
    const lines = markdown.replace(/\r\n/g, '\n').split('\n')
    let listItems: string[] = []

    const flushList = () => {
      if (listItems.length) {
        nodes.push(
          <ul key={`ul-${nodes.length}`}>
            {listItems.map((item, index) => <li key={index}>{renderInline(item)}</li>)}
          </ul>
        )
        listItems = []
      }
    }

    for (let index = 0; index < lines.length;) {
      const line = lines[index]

      if (line.startsWith('```')) {
        flushList()
        const language = line.slice(3).trim()
        const code: string[] = []
        index++
        while (index < lines.length && !lines[index].startsWith('```')) {
          code.push(lines[index])
          index++
        }
        index++
        nodes.push(
          <pre key={`pre-${nodes.length}`}>
            {language && <span className="study-code-language">{language}</span>}
            <code>{code.join('\n')}</code>
          </pre>
        )
        continue
      }

      if (/^#{1,6}\s/.test(line)) {
        flushList()
        const level = line.match(/^#+/)![0].length
        const text = line.replace(/^#{1,6}\s*/, '')
        const HeadingTag = { 1: 'h1', 2: 'h2', 3: 'h3', 4: 'h4', 5: 'h5', 6: 'h6' }[level] as keyof JSX.IntrinsicElements
        nodes.push(<HeadingTag key={`h-${nodes.length}`}>{renderInline(text)}</HeadingTag>)
        index++
        continue
      }

      if (/^[-*]\s/.test(line)) {
        listItems.push(line.replace(/^[-*]\s*/, ''))
        index++
        continue
      }

      flushList()
      if (!line.trim()) {
        index++
        continue
      }

      if (/^>\s/.test(line)) {
        nodes.push(<blockquote key={`q-${nodes.length}`}>{renderInline(line.replace(/^>\s*/, ''))}</blockquote>)
      } else {
        nodes.push(<p key={`p-${nodes.length}`}>{renderInline(line)}</p>)
      }
      index++
    }

    flushList()
    return nodes
  }, [markdown])

  return <div className="study-markdown">{blocks}</div>
}

export default function DocumentReaderPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [document, setDocument] = useState<CandidateDocumentDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    getStudyDocument(id)
      .then(setDocument)
      .catch(() => setError('Không thể mở tài liệu này.'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <p className="study-state">Đang mở tài liệu…</p>
  if (error || !document) return <p className="study-state study-error" role="alert">{error || 'Không tìm thấy tài liệu.'}</p>

  const legacyContentUrl = document.contentMd || ''
  const fileUrl = document.fileUrl || (/^(https?:\/\/|\/).*\.(pdf|doc|docx)(\?.*)?$/i.test(legacyContentUrl) ? legacyContentUrl : null)
  const content = document.content || document.contentMd || ''
  const htmlContent = isHtml(content)

  return (
    <div className="study-reader-page">
      <header className="study-reader-toolbar">
        <button type="button" className="study-back-button" onClick={() => navigate('/documents')}>
          <span aria-hidden="true">←</span> Quay lại danh sách
        </button>
        <div className="study-reader-meta">
          <span className="study-topic-badge">{topicLabel(document)}</span>
          <span>Cập nhật: {formatDate(document.updatedAt)}</span>
        </div>
      </header>

      <article className="study-reader">
        <h1>{document.title}</h1>

        {fileUrl ? (
          <div className="study-file-viewer">
            {isFileUrl(fileUrl) && <iframe src={fileUrl} title={document.title} />}
            <a className="study-download-button" href={fileUrl} target="_blank" rel="noreferrer">Tải tài liệu ↓</a>
          </div>
        ) : htmlContent ? (
          <iframe className="study-html-viewer" srcDoc={content} title={document.title} />
        ) : (
          <MarkdownContent markdown={content} />
        )}

        <aside className="study-practice-banner">
          <div>
            <span>KẾT THÚC BÀI ĐỌC</span>
            <h2>Đã hiểu chủ đề này?</h2>
            <p>Tiếp tục bằng một buổi phỏng vấn mô phỏng theo chủ đề tài liệu.</p>
          </div>
          <button
            type="button"
            onClick={() => navigate(`/interview/new?topicId=${encodeURIComponent(document.topicId || '')}&topicCode=${encodeURIComponent(document.topicCode || '')}`)}
          >
            Luyện phỏng vấn chủ đề này <span aria-hidden="true">→</span>
          </button>
        </aside>
      </article>
    </div>
  )
}
