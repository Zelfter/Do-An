/**
 * InterviewRoomPage — the candidate "Mock Interview Room" (Phần 2).
 *
 * Single-page room implementing the interview state machine (BR-16):
 *   CREATED → (start) → IN_PROGRESS → (end / time-out / pool exhausted) → ended → report
 *
 * Zones (per the agreed UI):
 *   - Header: topics, level, countdown timer (server-authoritative — NFR-16), progress
 *   - Chat window: transcript of AI questions + candidate answers (NO scores — BR-20/AC-05/AC-12)
 *   - Input area: textarea answer + Send / Skip
 *   - Controls: Start interview / End interview
 *
 * Integration: uses the shared apiClient (Bearer token auto-attached). Reaches Java
 * /api/v1 only. Timer pauses locally while the AI is grading and re-syncs from the
 * server on each poll — matching "timer pauses while AI works".
 */
import React, { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  endSession,
  getSessionState,
  newRequestId,
  skipTurn,
  startSession,
  submitAnswer,
  type InterviewSessionState,
} from '../../api/interviewApi'
import { interviewStyles as s } from '../../components/shared/interviewStyles'
import ConfirmDialog from '../../components/shared/ConfirmDialog'

// NFR-26: Vietnamese labels for enum values displayed to users.
const STATUS_LABELS: Record<string, string> = {
  CREATED: 'Chưa bắt đầu',
  IN_PROGRESS: 'Đang phỏng vấn',
  COMPLETED: 'Đã hoàn thành',
  TIMED_OUT: 'Hết giờ',
  CANCELLED: 'Đã hủy',
}
const LEVEL_LABELS: Record<string, string> = { JUNIOR: 'Sơ cấp', MID: 'Trung cấp', SENIOR: 'Cao cấp' }
const KIND_LABELS: Record<string, string> = { ORIGINAL: 'Câu hỏi', FOLLOW_UP: 'Câu hỏi phụ' }

const ENDED_STATUSES = ['COMPLETED', 'TIMED_OUT', 'CANCELLED']
function isEnded(status: string): boolean {
  return ENDED_STATUSES.includes(status)
}

function getStatus(err: unknown): number | null {
  if (err && typeof err === 'object' && 'response' in err && err.response && typeof err.response === 'object' && 'status' in err.response) {
    return (err.response as { status: number }).status
  }
  return null
}

/** mm:ss countdown formatting. */
function formatCountdown(sec: number | null): string {
  if (sec == null) return '--:--'
  const v = Math.max(0, Math.round(sec))
  const m = Math.floor(v / 60)
  const r = v % 60
  return `${String(m).padStart(2, '0')}:${String(r).padStart(2, '0')}`
}

export default function InterviewRoomPage() {
  const { sessionId = '' } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()

  const [session, setSession] = useState<InterviewSessionState | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [answer, setAnswer] = useState('')
  const [countdown, setCountdown] = useState<number | null>(null)
  const [confirmEndOpen, setConfirmEndOpen] = useState(false)

  // Wall-clock time the session reaches zero, synced from the server (NFR-16).
  const endsAtRef = useRef<number | null>(null)
  // Idempotency id for the current answer/skip attempt (BR-18 / idempotent answers).
  const requestIdRef = useRef<string>(newRequestId())
  const bottomRef = useRef<HTMLDivElement | null>(null)

  // ── Load & resume (NFR-16: resume after refresh) ──────────────────────────────
  const loadState = useCallback(
    async (opts?: { silent?: boolean }) => {
      try {
        const data = await getSessionState(sessionId)
        setSession(data)
        setError(null)
        // Re-sync authoritative remaining time.
        endsAtRef.current = Date.now() + data.remainingSec * 1000
        setCountdown(data.remainingSec)
        if (isEnded(data.status)) {
          navigate(`/interview/${sessionId}/report`, { replace: true })
        }
      } catch (err) {
        if (!opts?.silent) {
          const status = getStatus(err)
          if (status === 404) setError('Không tìm thấy phiên phỏng vấn.')
          else if (status === 403) setError('Bạn không có quyền truy cập phiên phỏng vấn này.')
          else setError('Không thể tải phiên phỏng vấn. Vui lòng thử lại.')
        }
      } finally {
        if (!opts?.silent) setLoading(false)
      }
    },
    [sessionId, navigate],
  )

  useEffect(() => {
    if (!sessionId) {
      setError('Thiếu mã phiên phỏng vấn.')
      setLoading(false)
      return
    }
    loadState()
  }, [sessionId, loadState])

  // ── Local countdown tick (paused while AI grades — "timer pauses while AI works") ─
  useEffect(() => {
    if (session?.status !== 'IN_PROGRESS') {
      setCountdown(null)
      return
    }
    const tick = window.setInterval(() => {
      if (submitting) return // freeze display while the AI is evaluating
      if (endsAtRef.current == null) return
      const remaining = Math.max(0, Math.round((endsAtRef.current - Date.now()) / 1000))
      setCountdown(remaining)
    }, 1000)
    return () => window.clearInterval(tick)
  }, [session?.status, submitting])

  // ── Resync from server periodically + on tab refocus (NFR-16) ──────────────────
  useEffect(() => {
    if (session?.status !== 'IN_PROGRESS') return
    const poll = () => loadState({ silent: true })
    const id = window.setInterval(poll, 15000)
    const onVis = () => {
      if (document.visibilityState === 'visible') poll()
    }
    document.addEventListener('visibilitychange', onVis)
    return () => {
      window.clearInterval(id)
      document.removeEventListener('visibilitychange', onVis)
    }
  }, [session?.status, loadState])

  // ── Auto-scroll chat to the latest question ──────────────────────────────────
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [session?.turns.length, session?.currentTurn?.turnId])

  // ── Actions ───────────────────────────────────────────────────────────────────
  async function handleStart() {
    setActionError(null)
    setSubmitting(true)
    try {
      const data = await startSession(sessionId)
      setSession(data)
      endsAtRef.current = Date.now() + data.remainingSec * 1000
      setCountdown(data.remainingSec)
    } catch (err) {
      const status = getStatus(err)
      setActionError(status === 409 ? 'Phiên đã bắt đầu. Đang tải lại…' : 'Không thể bắt đầu phỏng vấn. Vui lòng thử lại.')
      if (status === 409) loadState({ silent: true })
    } finally {
      setSubmitting(false)
    }
  }

  async function handleSubmitAnswer() {
    const turn = session?.currentTurn
    if (!turn || !answer.trim() || submitting) return
    setActionError(null)
    setSubmitting(true)
    const rid = requestIdRef.current
    try {
      const data = await submitAnswer(sessionId, turn.turnId, rid, answer.trim())
      setSession(data)
      setAnswer('')
      endsAtRef.current = Date.now() + data.remainingSec * 1000
      setCountdown(data.remainingSec)
      // Fresh id for the next question.
      requestIdRef.current = newRequestId()
      if (isEnded(data.status)) navigate(`/interview/${sessionId}/report`, { replace: true })
    } catch (err) {
      const status = getStatus(err)
      if (status === 409) {
        // Already answered (idempotent replay) — reload state, drop the draft.
        setAnswer('')
        requestIdRef.current = newRequestId()
        loadState({ silent: true })
      } else {
        // Keep the draft so the candidate can retry with the SAME requestId.
        setActionError('Gửi câu trả lời thất bại. Vui lòng thử lại.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleSkip() {
    const turn = session?.currentTurn
    if (!turn || submitting) return
    setActionError(null)
    setSubmitting(true)
    const rid = requestIdRef.current
    try {
      const data = await skipTurn(sessionId, turn.turnId, rid)
      setSession(data)
      setAnswer('')
      endsAtRef.current = Date.now() + data.remainingSec * 1000
      setCountdown(data.remainingSec)
      requestIdRef.current = newRequestId()
      if (isEnded(data.status)) navigate(`/interview/${sessionId}/report`, { replace: true })
    } catch (err) {
      const status = getStatus(err)
      if (status === 409) {
        requestIdRef.current = newRequestId()
        loadState({ silent: true })
      } else {
        setActionError('Không thể bỏ qua câu hỏi. Vui lòng thử lại.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleEnd() {
    setConfirmEndOpen(false)
    setActionError(null)
    setSubmitting(true)
    try {
      await endSession(sessionId)
      navigate(`/interview/${sessionId}/report`, { replace: true })
    } catch (err) {
      setActionError('Kết thúc phỏng vấn thất bại. Vui lòng thử lại.')
      loadState({ silent: true })
    } finally {
      setSubmitting(false)
    }
  }

  // ── Render ─────────────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <main style={{ ...s.page, alignItems: 'center', justifyContent: 'center' }}>
        <p style={s.mutedText} aria-live="polite">Đang tải phiên phỏng vấn…</p>
      </main>
    )
  }

  if (error && !session) {
    return (
      <main style={s.page}>
        <div style={{ ...s.pageNarrow, marginTop: '3rem' }}>
          <div style={s.card}>
            <h1 style={s.sectionTitle}>Phòng phỏng vấn</h1>
            <p style={s.errorText} role="alert">{error}</p>
            <button type="button" style={s.primaryButton} onClick={() => navigate('/dashboard')}>
              Về trang chủ ứng viên
            </button>
          </div>
        </div>
      </main>
    )
  }

  if (!session) return null

  const inProgress = session.status === 'IN_PROGRESS'
  const created = session.status === 'CREATED'
  const lowTime = countdown != null && countdown <= 60
  const current = session.currentTurn

  return (
    <main style={s.page}>
      {/* ── Header: session info + countdown ─────────────────────────────────── */}
      <header style={styles.header}>
        <div style={styles.headerLeft}>
          <span style={styles.headerBrand}>Phòng phỏng vấn</span>
          <span style={styles.headerBadge}>{LEVEL_LABELS[session.level] ?? session.level}</span>
          {session.topics.length > 0 && (
            <span style={styles.headerTopics}>
              {session.topics.map((t) => t.name).join(' · ')}
            </span>
          )}
        </div>
        <div style={styles.headerRight} aria-live="polite">
          <span style={styles.timerLabel}>Thời gian còn lại</span>
          <span style={{ ...styles.timer, ...(lowTime ? styles.timerLow : {}) }}>
            ⏱ {formatCountdown(inProgress ? countdown : created ? session.timeLimitSec : 0)}
          </span>
          {inProgress && (
            <span style={styles.progress}>
              {session.answeredCount}/{session.totalQuestions} câu
            </span>
          )}
        </div>
      </header>

      <div style={styles.body}>
        {actionError && (
          <p style={{ ...s.errorText, textAlign: 'center' }} role="alert">{actionError}</p>
        )}

        {/* ── CREATED: ready / start panel ───────────────────────────────────── */}
        {created && (
          <section style={s.card}>
            <h1 style={{ ...s.sectionTitle, fontSize: '1.25rem' }}>Sẵn sàng bắt đầu phỏng vấn</h1>
            <p style={s.mutedText}>
              Hệ thống đã chuẩn bị {session.totalQuestions} câu hỏi từ ngân hàng, bám theo JD và chủ đề bạn chọn.
            </p>
            <div style={{ marginTop: '1rem' }}>
              <p style={s.label}>Chủ đề phỏng vấn</p>
              <div style={s.chipList}>
                {session.topics.map((t) => (
                  <span key={t.topicId} style={s.chip}>{t.name}</span>
                ))}
              </div>
            </div>
            <div style={{ display: 'flex', gap: '1.5rem', marginTop: '1rem', flexWrap: 'wrap' }}>
              <div>
                <p style={s.mutedText}>Cấp độ</p>
                <p style={{ fontWeight: 600 }}>{LEVEL_LABELS[session.level] ?? session.level}</p>
              </div>
              <div>
                <p style={s.mutedText}>Thời lượng</p>
                <p style={{ fontWeight: 600 }}>{formatCountdown(session.timeLimitSec)}</p>
              </div>
              <div>
                <p style={s.mutedText}>Số câu hỏi</p>
                <p style={{ fontWeight: 600 }}>{session.totalQuestions}</p>
              </div>
            </div>
            <div style={{ marginTop: '1.25rem', display: 'flex', gap: '0.75rem' }}>
              <button
                type="button"
                style={{ ...s.primaryButton, ...(submitting ? s.disabledButton : {}) }}
                onClick={handleStart}
                disabled={submitting}
                aria-busy={submitting}
              >
                {submitting ? 'Đang bắt đầu…' : 'Bắt đầu phỏng vấn'}
              </button>
              <button type="button" style={s.secondaryButton} onClick={() => navigate('/dashboard')}>
                Hủy
              </button>
            </div>
            <p style={{ ...s.mutedText, marginTop: '1rem' }}>
              Lưu ý: đồng hồ sẽ đếm ngược từ lúc bắt đầu. Bạn có thể kết thúc sớm bất cứ lúc nào.
            </p>
          </section>
        )}

        {/* ── IN_PROGRESS: chat + input + controls ──────────────────────────── */}
        {inProgress && (
          <>
            <section
              style={styles.chatWindow}
              aria-label="Cuộc trò chuyện phỏng vấn"
              aria-live="polite"
            >
              {session.turns.map((t) => (
                <div key={t.turnId} style={styles.turnGroup}>
                  {/* AI question bubble */}
                  <div style={styles.bubbleRowAi}>
                    <div style={styles.bubbleAi}>
                      <div style={styles.bubbleAuthor}>
                        🤖 AI · {KIND_LABELS[t.kind] ?? t.kind}
                        {t.topicName && <span style={styles.topicTag}>{t.topicName}</span>}
                      </div>
                      <div style={styles.bubbleText}>{t.questionText}</div>
                    </div>
                  </div>
                  {/* Candidate answer bubble (if answered) */}
                  {t.answerText != null && (
                    <div style={styles.bubbleRowYou}>
                      <div style={styles.bubbleYou}>
                        <div style={styles.bubbleAuthor}>Bạn</div>
                        <div style={styles.bubbleText}>{t.answerText}</div>
                      </div>
                    </div>
                  )}
                </div>
              ))}
              <div ref={bottomRef} />
            </section>

            {/* Input area */}
            <section style={s.card}>
              {current ? (
                <>
                  <p style={{ ...s.label, marginBottom: '0.4rem' }}>
                    {KIND_LABELS[current.kind] ?? 'Câu hỏi'}
                    {current.topicName && <span style={styles.topicTag}>{current.topicName}</span>}
                  </p>
                  <textarea
                    style={s.textarea}
                    placeholder="Nhập câu trả lời của bạn ở đây…"
                    value={answer}
                    onChange={(e) => setAnswer(e.target.value)}
                    disabled={submitting}
                    aria-label="Câu trả lời của bạn"
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
                        e.preventDefault()
                        handleSubmitAnswer()
                      }
                    }}
                  />
                  <div style={styles.inputActions}>
                    <button
                      type="button"
                      style={{ ...s.primaryButton, ...(!answer.trim() || submitting ? s.disabledButton : {}) }}
                      onClick={handleSubmitAnswer}
                      disabled={!answer.trim() || submitting}
                      aria-busy={submitting}
                    >
                      {submitting ? 'Đang chấm điểm…' : 'Gửi câu trả lời'}
                    </button>
                    <button
                      type="button"
                      style={{ ...s.secondaryButton, ...(submitting ? s.disabledButton : {}) }}
                      onClick={handleSkip}
                      disabled={submitting}
                    >
                      Bỏ qua
                    </button>
                    <span style={s.mutedText}>Ctrl/⌘ + Enter để gửi nhanh</span>
                  </div>
                  {submitting && (
                    <p style={s.infoText} aria-live="polite">
                      Đang chấm điểm câu trả lời… (đồng hồ tạm dừng)
                    </p>
                  )}
                </>
              ) : (
                <p style={s.mutedText}>Đang chuẩn bị câu hỏi tiếp theo…</p>
              )}

              {/* Controls */}
              <div style={styles.controls}>
                <button
                  type="button"
                  style={{ ...s.dangerButton, ...(submitting ? s.disabledButton : {}) }}
                  onClick={() => setConfirmEndOpen(true)}
                  disabled={submitting}
                >
                  Kết thúc phỏng vấn
                </button>
              </div>
            </section>
          </>
        )}

        {/* ── Ended (should normally redirect to report; fallback) ────────────── */}
        {isEnded(session.status) && (
          <section style={s.card}>
            <p style={{ fontWeight: 600 }}>Phiên phỏng vấn đã kết thúc — {STATUS_LABELS[session.status]}.</p>
            <button type="button" style={s.primaryButton} onClick={() => navigate(`/interview/${sessionId}/report`)}>
              Xem kết quả & nhận xét
            </button>
          </section>
        )}
      </div>

      {confirmEndOpen && (
        <ConfirmDialog
          title="Kết thúc phỏng vấn?"
          message="Bạn sẽ không thể trả lời thêm. Hệ thống sẽ chấm điểm các câu đã trả lời và tạo báo cáo."
          confirmLabel="Kết thúc"
          onConfirm={handleEnd}
          onCancel={() => setConfirmEndOpen(false)}
        />
      )}
    </main>
  )
}

// ── Room-specific inline styles ───────────────────────────────────────────────
const styles: Record<string, React.CSSProperties> = {
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '1rem',
    padding: '0.75rem 1.5rem',
    background: '#fff',
    borderBottom: '1px solid #ececf3',
    flexWrap: 'wrap',
  },
  headerLeft: { display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' },
  headerBrand: { fontWeight: 700, fontSize: '1.05rem', color: '#1f2937' },
  headerBadge: { ...s.badge, ...s.badgeInfo },
  headerTopics: { fontSize: '0.85rem', color: '#6b7280' },
  headerRight: { display: 'flex', alignItems: 'center', gap: '0.85rem' },
  timerLabel: { fontSize: '0.8rem', color: '#6b7280' },
  timer: { fontVariantNumeric: 'tabular-nums', fontSize: '1.4rem', fontWeight: 700, color: '#1f2937' },
  timerLow: { color: '#dc2626' },
  progress: { fontSize: '0.85rem', color: '#6b7280' },

  body: { flex: 1, maxWidth: 820, margin: '0 auto', width: '100%', padding: '1.25rem 1.5rem', boxSizing: 'border-box' },

  chatWindow: {
    background: '#fff',
    borderRadius: 12,
    padding: '1rem 1.25rem',
    marginBottom: '1rem',
    height: '46vh',
    overflowY: 'auto',
    boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
  },
  turnGroup: { display: 'flex', flexDirection: 'column', gap: '0.4rem' },
  bubbleRowAi: { display: 'flex', justifyContent: 'flex-start' },
  bubbleRowYou: { display: 'flex', justifyContent: 'flex-end' },
  bubbleAi: {
    background: '#f3f4f6', borderRadius: '12px 12px 12px 2px', padding: '0.6rem 0.85rem',
    maxWidth: '82%', color: '#1f2937',
  },
  bubbleYou: {
    background: '#4f46e5', borderRadius: '12px 12px 2px 12px', padding: '0.6rem 0.85rem',
    maxWidth: '82%', color: '#fff',
  },
  bubbleAuthor: { fontSize: '0.75rem', fontWeight: 600, opacity: 0.85, marginBottom: 2, display: 'flex', alignItems: 'center', gap: '0.4rem' },
  bubbleText: { fontSize: '0.95rem', lineHeight: 1.5, whiteSpace: 'pre-wrap' },
  topicTag: { background: 'rgba(79,70,229,0.12)', color: '#4338ca', borderRadius: 999, padding: '0.05rem 0.45rem', fontSize: '0.7rem', fontWeight: 600 },

  inputActions: { display: 'flex', alignItems: 'center', gap: '0.6rem', marginTop: '0.6rem', flexWrap: 'wrap' },
  controls: { marginTop: '1rem', display: 'flex', justifyContent: 'flex-end' },
}
