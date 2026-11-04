/**
 * InterviewReportPage — post-session results & feedback (FR-25/26/43, Step 4).
 *
 * Loads the full report (scores are only available after the session ends —
 * BR-20). Shows total score, classification, weak topics, study materials and a
 * per-turn breakdown with the rubric criteria.
 */
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getSessionReport, type SessionReport } from '../../api/interviewApi'
import { interviewStyles as s } from '../../components/shared/interviewStyles'

const LEVEL_LABELS: Record<string, string> = { JUNIOR: 'Sơ cấp', MID: 'Trung cấp', SENIOR: 'Cao cấp' }
const KIND_LABELS: Record<string, string> = { ORIGINAL: 'Câu hỏi', FOLLOW_UP: 'Câu hỏi phụ' }
const STATUS_LABELS: Record<string, string> = {
  PENDING: 'Chờ trả lời', ANSWERED: 'Đã trả lời', SKIPPED: 'Đã bỏ qua',
  TIMED_OUT: 'Hết giờ', UNGRADED: 'Chưa chấm được',
}

function getStatus(err: unknown): number | null {
  if (err && typeof err === 'object' && 'response' in err && err.response && typeof err.response === 'object' && 'status' in err.response) {
    return (err.response as { status: number }).status
  }
  return null
}

function num(v: number | null | undefined, suffix = ''): string {
  return v == null ? '—' : `${v.toFixed(2)}${suffix}`
}

export default function InterviewReportPage() {
  const { sessionId = '' } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()
  const [report, setReport] = useState<SessionReport | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!sessionId) {
      setError('Thiếu mã phiên phỏng vấn.')
      setLoading(false)
      return
    }
    getSessionReport(sessionId)
      .then((data) => { setReport(data); setLoading(false) })
      .catch((err) => {
        const status = getStatus(err)
        if (status === 409) setError('Báo cáo chỉ khả dụng sau khi kết thúc phỏng vấn.')
        else if (status === 404) setError('Không tìm thấy phiên phỏng vấn.')
        else if (status === 403) setError('Bạn không có quyền truy cập phiên này.')
        else setError('Không thể tải báo cáo. Vui lòng thử lại.')
        setLoading(false)
      })
  }, [sessionId])

  if (loading) {
    return <p style={s.mutedText} aria-live="polite">Đang tải báo cáo…</p>
  }

  if (error && !report) {
    return (
      <>
        <p style={s.errorText} role="alert">{error}</p>
        <button type="button" style={s.primaryButton} onClick={() => navigate('/dashboard')}>Về trang chủ ứng viên</button>
      </>
    )
  }

  if (!report) return null

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
        <h1 style={{ margin: 0, fontSize: '1.4rem' }}>Kết quả & nhận xét</h1>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button type="button" style={s.primaryButton} onClick={() => navigate('/interview/new')}>Làm phỏng vấn mới</button>
          <button type="button" style={s.secondaryButton} onClick={() => navigate('/dashboard')}>Về trang chủ</button>
        </div>
      </div>

      {/* Summary card */}
      <div style={s.card}>
        <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <div>
            <p style={s.mutedText}>Tổng điểm</p>
            <p style={{ fontSize: '2rem', fontWeight: 700, margin: 0 }}>{num(report.totalScore, '/10')}</p>
          </div>
          <div>
            <p style={s.mutedText}>Xếp loại</p>
            <p style={{ fontSize: '1.25rem', fontWeight: 600, margin: 0 }}>{report.classification || '—'}</p>
          </div>
          <div>
            <p style={s.mutedText}>Cấp độ</p>
            <p style={{ fontWeight: 600, margin: 0 }}>{LEVEL_LABELS[report.level] ?? report.level}</p>
          </div>
          <div>
            <p style={s.mutedText}>Trạng thái báo cáo</p>
            <p style={{ margin: 0 }}>
              <span style={{ ...s.badge, ...(report.reportStatus === 'READY' ? s.badgeSuccess : s.badgeWarn) }}>
                {report.reportStatus === 'READY' ? 'Hoàn chỉnh' : 'Một phần'}
              </span>
            </p>
          </div>
        </div>
        {report.summary && (
          <p style={{ marginTop: '1rem', lineHeight: 1.6 }}>{report.summary}</p>
        )}
        {report.reportStatus === 'PARTIAL' && (
          <p style={{ ...s.mutedText, marginTop: '0.5rem' }}>
            Báo cáo ở chế độ “Một phần”: điểm số và chủ đề yếu vẫn hiển thị dù phần văn bản tóm tắt gặp lỗi.
          </p>
        )}
      </div>

      {/* Topics + weak topics */}
      <div style={s.card}>
        <h2 style={{ ...s.sectionTitle, fontSize: '1.05rem' }}>Kết quả theo chủ đề</h2>
        {report.topics.length === 0 ? (
          <p style={s.mutedText}>Không có dữ liệu chủ đề.</p>
        ) : (
          <table style={s.table}>
            <thead>
              <tr>
                <th style={s.th}>Chủ đề</th>
                <th style={s.th}>Điểm</th>
                <th style={s.th}>Đánh giá</th>
              </tr>
            </thead>
            <tbody>
              {report.topics.map((t) => (
                <tr key={t.topicId}>
                  <td style={s.td}>{t.name}{t.code ? ` (${t.code})` : ''}</td>
                  <td style={s.td}>{t.topicScore != null ? `${t.topicScore.toFixed(2)}/10` : '—'}</td>
                  <td style={s.td}>
                    {t.isWeak
                      ? <span style={{ ...s.badge, ...s.badgeDanger }}>Cần củng cố</span>
                      : <span style={{ ...s.badge, ...s.badgeSuccess }}>Đạt</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Study materials for weak topics (FR-25/28) */}
      {report.studyMaterials.length > 0 && (
        <div style={s.card}>
          <h2 style={{ ...s.sectionTitle, fontSize: '1.05rem' }}>Tài liệu ôn tập cho chủ đề yếu</h2>
          <ul style={{ margin: 0, paddingLeft: '1.25rem', lineHeight: 1.8 }}>
            {report.studyMaterials.map((m) => (
              <li key={m.documentId}>
                <strong>{m.topicName}</strong>: {m.title}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Per-turn breakdown */}
      <div style={s.card}>
        <h2 style={{ ...s.sectionTitle, fontSize: '1.05rem' }}>Chi tiết từng câu</h2>
        {report.turns.length === 0 ? (
          <p style={s.mutedText}>Không có lượt trả lời.</p>
        ) : (
          report.turns.map((t, i) => (
            <div key={t.turnId} style={{ borderBottom: '1px solid #ececf3', paddingBottom: '0.75rem', marginBottom: '0.75rem' }}>
              <p style={{ fontWeight: 600, margin: '0 0 0.25rem' }}>
                {i + 1}. {KIND_LABELS[t.kind] ?? t.kind}
                <span style={{ ...s.badge, ...s.badgeInfo, marginLeft: '0.5rem' }}>{STATUS_LABELS[t.status] ?? t.status}</span>
              </p>
              <p style={{ margin: '0 0 0.25rem' }}><strong>Câu hỏi:</strong> {t.questionText}</p>
              {t.answerText ? (
                <p style={{ margin: '0 0 0.25rem' }}><strong>Trả lời:</strong> {t.answerText}</p>
              ) : (
                <p style={{ ...s.mutedText, margin: '0 0 0.25rem' }}><strong>Trả lời:</strong> (bỏ qua / không trả lời)</p>
              )}
              <p style={{ margin: '0.25rem 0', fontSize: '0.9rem' }}>
                Điểm: chính xác {num(t.accuracy)} · đầy đủ {num(t.completeness)} · rõ ràng {num(t.clarity)} · ví dụ {num(t.exampleScore)}
                {' → '}
                <strong>{num(t.totalScore, '/10')}</strong>
              </p>
              {t.missedKeyPoints.length > 0 && (
                <p style={{ ...s.mutedText, margin: '0.25rem 0', fontSize: '0.85rem' }}>
                  Điểm còn thiếu: {t.missedKeyPoints.join('; ')}
                </p>
              )}
              {t.feedback && <p style={{ margin: '0.25rem 0', fontSize: '0.9rem' }}>Nhận xét: {t.feedback}</p>}
            </div>
          ))
        )}
      </div>
    </>
  )
}
