/**
 * CandidateDashboard — candidate home (FR-24, Step 4).
 *
 * Lists the candidate's interview sessions and routes to the room (ongoing)
 * or the report (ended). Entry point of the candidate navigation flow.
 */
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listSessions, type SessionSummaryView } from '../../api/interviewApi'
import { interviewStyles as s } from '../../components/shared/interviewStyles'

const STATUS_LABELS: Record<string, string> = {
  CREATED: 'Chưa bắt đầu',
  IN_PROGRESS: 'Đang phỏng vấn',
  COMPLETED: 'Đã hoàn thành',
  TIMED_OUT: 'Hết giờ',
  CANCELLED: 'Đã hủy',
}
const LEVEL_LABELS: Record<string, string> = { JUNIOR: 'Sơ cấp', MID: 'Trung cấp', SENIOR: 'Cao cấp' }
const ENDED_STATUSES = ['COMPLETED', 'TIMED_OUT', 'CANCELLED']

export default function CandidateDashboard() {
  const navigate = useNavigate()
  const [sessions, setSessions] = useState<SessionSummaryView[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    listSessions()
      .then((data) => { setSessions(data); setLoading(false) })
      .catch(() => { setError('Không tải được danh sách phiên phỏng vấn.'); setLoading(false) })
  }, [])

  function openSession(item: SessionSummaryView) {
    if (ENDED_STATUSES.includes(item.status)) {
      navigate(`/interview/${item.sessionId}/report`)
    } else {
      navigate(`/interview/${item.sessionId}`)
    }
  }

  return (
    <>
      <section className="candidate-hero">
        <div className="candidate-hero-copy">
          <span className="candidate-kicker">KHÔNG GIAN LUYỆN TẬP</span>
          <h1>Sẵn sàng cho buổi phỏng vấn tiếp theo?</h1>
          <p>Dán JD, chọn cấp độ và nhận một phòng phỏng vấn được cá nhân hóa theo nhu cầu của bạn.</p>
          <button type="button" className="btn btn-primary candidate-hero-button" onClick={() => navigate('/interview/new')} style={{ marginTop: 22 }}>
            + Tạo phỏng vấn mới
          </button>
        </div>
        <div className="candidate-hero-visual" aria-hidden="true">
          <span className="hero-spark one">✦</span>
          <span className="hero-spark two">✦</span>
          <div className="hero-glass-card">
            <div className="hero-check">✓</div>
            <div className="hero-profile-row">
              <div className="hero-profile-avatar">CV</div>
              <div className="hero-profile-lines" style={{ flex: 1 }}><span /><span /><span /></div>
            </div>
            <div className="hero-score"><span /><span /><span /></div>
          </div>
        </div>
      </section>

      <section className="card history-card">
        <div className="history-card-head">
          <h2>Lịch sử phỏng vấn</h2>
          {!loading && <span className="history-count">{sessions.length} phiên</span>}
        </div>

        {loading && <p style={{ ...s.mutedText, padding: '24px 20px' }}>Đang tải…</p>}
        {error && <p style={{ ...s.errorText, margin: 20 }} role="alert">{error}</p>}

        {!loading && !error && sessions.length === 0 && (
          <div className="history-empty">
            <svg width="92" height="92" viewBox="0 0 92 92" fill="none" aria-hidden="true">
              <rect x="17" y="12" width="58" height="68" rx="12" fill="#EEF2FF" stroke="#C7D2FE" strokeWidth="2" />
              <rect x="25" y="20" width="42" height="52" rx="7" fill="#fff" />
              <path d="M34 34h24M34 43h18M34 52h24M34 61h14" stroke="#818CF8" strokeWidth="3" strokeLinecap="round" />
              <circle cx="67" cy="67" r="12" fill="#10B981" stroke="#fff" strokeWidth="3" />
              <path d="M62 67l3.5 3.5L72 64" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
              <path d="M75 18l2 5 5 2-5 2-2 5-2-5-5-2 5-2 2-5z" fill="#A78BFA" />
            </svg>
            <h3>Chưa có buổi phỏng vấn nào</h3>
            <p>Tạo buổi phỏng vấn đầu tiên để bắt đầu hành trình luyện tập.</p>
            <button type="button" className="btn history-empty-button" onClick={() => navigate('/interview/new')}>+ Tạo buổi phỏng vấn mới</button>
          </div>
        )}

        {!loading && !error && sessions.length > 0 && (
          <div className="history-table-wrap">
            <table className="history-table">
              <thead>
                <tr><th>Cấp độ</th><th>Trạng thái</th><th>Tổng điểm</th><th>Ngày tạo</th><th></th></tr>
              </thead>
              <tbody>
                {sessions.map((item) => {
                  const ended = ENDED_STATUSES.includes(item.status)
                  return (
                    <tr key={item.sessionId}>
                      <td>{LEVEL_LABELS[item.level] ?? item.level}</td>
                      <td><span className={'badge ' + (ended ? 'badge-success' : 'badge-info')}>{STATUS_LABELS[item.status] ?? item.status}</span></td>
                      <td><strong>{item.totalScore != null ? `${item.totalScore.toFixed(2)}/10` : '—'}</strong></td>
                      <td>{new Date(item.createdAt).toLocaleString('vi-VN')}</td>
                      <td><button type="button" className="btn btn-secondary btn-sm" onClick={() => openSession(item)}>{ended ? 'Xem kết quả' : 'Tiếp tục'}</button></td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  )
}
