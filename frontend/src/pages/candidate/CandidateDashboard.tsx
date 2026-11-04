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
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <h1 style={{ margin: 0, fontSize: '1.4rem' }}>Trang ứng viên</h1>
        <button type="button" style={s.primaryButton} onClick={() => navigate('/interview/new')}>
          + Tạo phỏng vấn mới
        </button>
      </div>

      <div style={s.card}>
        <h2 style={{ ...s.sectionTitle, fontSize: '1.05rem' }}>Lịch sử phỏng vấn</h2>

        {loading && <p style={s.mutedText}>Đang tải…</p>}
        {error && <p style={s.errorText} role="alert">{error}</p>}

        {!loading && !error && sessions.length === 0 && (
          <p style={s.mutedText}>
            Bạn chưa có phiên phỏng vấn nào. Bấm “Tạo phỏng vấn mới” để bắt đầu.
          </p>
        )}

        {!loading && !error && sessions.length > 0 && (
          <table style={s.table}>
            <thead>
              <tr>
                <th style={s.th}>Cấp độ</th>
                <th style={s.th}>Trạng thái</th>
                <th style={s.th}>Tổng điểm</th>
                <th style={s.th}>Ngày tạo</th>
                <th style={s.th}></th>
              </tr>
            </thead>
            <tbody>
              {sessions.map((item) => {
                const ended = ENDED_STATUSES.includes(item.status)
                return (
                  <tr key={item.sessionId}>
                    <td style={s.td}>{LEVEL_LABELS[item.level] ?? item.level}</td>
                    <td style={s.td}>
                      <span style={{ ...s.badge, ...(ended ? s.badgeSuccess : s.badgeInfo) }}>
                        {STATUS_LABELS[item.status] ?? item.status}
                      </span>
                    </td>
                    <td style={s.td}>
                      {item.totalScore != null ? `${item.totalScore.toFixed(2)}/10` : '—'}
                    </td>
                    <td style={s.td}>{new Date(item.createdAt).toLocaleString('vi-VN')}</td>
                    <td style={s.td}>
                      <button
                        type="button"
                        style={s.secondaryButton}
                        onClick={() => openSession(item)}
                      >
                        {ended ? 'Xem kết quả' : 'Tiếp tục'}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

      <p style={{ ...s.mutedText, marginTop: '0.75rem' }}>
        Dán JD của bạn, hệ thống sẽ phân tích chủ đề và cấp độ, rồi dựng một phòng phỏng vấn bám theo JD.
      </p>
    </>
  )
}
