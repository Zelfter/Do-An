/**
 * NewInterviewPage — JD → analyse → configure → create session (FR-06/07/09/10).
 *
 * Two-step flow:
 *   1) Paste JD and analyse → AI returns level + mapped topics (FR-06).
 *   2) Review/adjust level, time limit and selected topics → create session
 *      (FR-09/15) and navigate to the interview room.
 */
import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  analyzeJd,
  createSession,
  type JdAnalysisView,
  type Level,
} from '../../api/interviewApi'
import { interviewStyles as s } from '../../components/shared/interviewStyles'
import { IconUploadCloud } from '../../components/ui/icons'

const LEVELS: Level[] = ['JUNIOR', 'MID', 'SENIOR']
const LEVEL_LABELS: Record<string, string> = { JUNIOR: 'Sơ cấp', MID: 'Trung cấp', SENIOR: 'Cao cấp' }
const TIME_OPTIONS = [
  { label: '30 phút', sec: 1800 },
  { label: '60 phút', sec: 3600 },
  { label: '90 phút', sec: 5400 },
  { label: '120 phút', sec: 7200 },
]

const QUICK_SAMPLES: Record<string, string> = {
  'Phần mềm Java': 'Vị trí Java Backend Developer: phát triển RESTful API bằng Spring Boot, làm việc với PostgreSQL, JPA/Hibernate, Docker, Microservices, bảo mật JWT và viết unit test bằng JUnit/Mockito.',
  'Frontend React': 'Vị trí Frontend Developer: xây dựng ứng dụng React và TypeScript, quản lý state, responsive UI, REST API, tối ưu hiệu năng, accessibility và kiểm thử bằng Vitest.',
  'Data Analyst': 'Vị trí Data Analyst: thu thập và làm sạch dữ liệu bằng SQL/Python, xây dựng dashboard, phân tích xu hướng, báo cáo KPI, thống kê và trực quan hóa dữ liệu.',
}

function getStatus(err: unknown): number | null {
  if (err && typeof err === 'object' && 'response' in err && err.response && typeof err.response === 'object' && 'status' in err.response) {
    return (err.response as { status: number }).status
  }
  return null
}

export default function NewInterviewPage() {
  const navigate = useNavigate()

  const [step, setStep] = useState<'input' | 'config'>('input')
  const [rawText, setRawText] = useState('')
  const [analysis, setAnalysis] = useState<JdAnalysisView | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Config state (filled after analysis)
  const [level, setLevel] = useState<Level>('MID')
  const [timeLimitSec, setTimeLimitSec] = useState(3600)
  const [selected, setSelected] = useState<Record<string, boolean>>({})

  const fileInputRef = useRef<HTMLInputElement>(null)
  const [fileName, setFileName] = useState('')
  const [dragging, setDragging] = useState(false)

  async function handleAnalyze() {
    if (!rawText.trim()) {
      setError('Vui lòng dán nội dung JD.')
      return
    }
    setError(null)
    setLoading(true)
    try {
      const result = await analyzeJd(rawText.trim())
      setAnalysis(result)
      setLevel(result.level)
      // Default-select only terms mapped to a known topic.
      const sel: Record<string, boolean> = {}
      result.terms.forEach((t) => { if (t.topicId) sel[t.topicId] = true })
      setSelected(sel)
      setStep('config')
    } catch (err) {
      const status = getStatus(err)
      setError(status === 422 ? 'Phân tích JD thất bại. Vui lòng thử lại.' : 'Không thể phân tích JD. Vui lòng thử lại.')
    } finally {
      setLoading(false)
    }
  }

  async function handleStart() {
    if (!analysis) return
    const selectedIds = Object.keys(selected).filter((id) => selected[id])
    if (selectedIds.length === 0) {
      setError('Vui lòng chọn ít nhất một chủ đề.')
      return
    }
    setError(null)
    setLoading(true)
    try {
      const state = await createSession({
        jdId: analysis.jdId,
        level,
        timeLimitSec,
        selectedTopicIds: selectedIds,
      })
      navigate(`/interview/${state.sessionId}`)
    } catch (err) {
      const status = getStatus(err)
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? (status === 400 ? 'Không thể tạo phiên. Kiểm tra lại chủ đề đã chọn.' : 'Tạo phiên phỏng vấn thất bại. Vui lòng thử lại.'))
    } finally {
      setLoading(false)
    }
  }

  async function handleFile(file?: File) {
    if (!file) return
    const allowed = /\.(pdf|docx?)$/i.test(file.name)
    if (!allowed) {
      setError('Chỉ hỗ trợ tệp PDF, DOC hoặc DOCX.')
      return
    }
    setError(null)
    setFileName(file.name)
    if (/\.txt$/i.test(file.name)) {
      setRawText((await file.text()).slice(0, 5000))
    } else {
      setError(`Đã chọn ${file.name}. Hãy dán nội dung JD; trình duyệt chưa hỗ trợ trích xuất PDF/DOCX trực tiếp.`)
    }
  }

  return (
    <>
      <h1 style={{ margin: '0 0 1rem', fontSize: '1.4rem' }}>Tạo phỏng vấn mới</h1>

      {error && <p style={s.errorText} role="alert">{error}</p>}

      {step === 'input' && (
        <div className="interview-card">
          <div style={{ marginBottom: 18 }}>
            <h2 style={{ margin: 0, fontSize: '1.05rem', color: '#0f172a' }}>Nội dung JD (Job Description)</h2>
            <p style={{ margin: '6px 0 0', color: '#64748b', fontSize: '.88rem' }}>Tải tệp hoặc nhập mô tả công việc để hệ thống phân tích.</p>
          </div>

          <input ref={fileInputRef} type="file" accept=".pdf,.doc,.docx" style={{ display: 'none' }} onChange={(event) => handleFile(event.target.files?.[0])} />
          <div
            className={'jd-dropzone' + (dragging ? ' dragging' : '')}
            role="button"
            tabIndex={0}
            onClick={() => fileInputRef.current?.click()}
            onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') fileInputRef.current?.click() }}
            onDragEnter={(event) => { event.preventDefault(); setDragging(true) }}
            onDragOver={(event) => event.preventDefault()}
            onDragLeave={() => setDragging(false)}
            onDrop={(event) => { event.preventDefault(); setDragging(false); handleFile(event.dataTransfer.files?.[0]) }}
          >
            <div className="jd-upload-icon"><IconUploadCloud size={24} /></div>
            <strong>Dán toàn bộ mô tả công việc hoặc Tải lên tệp (.pdf, .docx)</strong>
            {fileName && <div className="jd-file-name">Đã chọn: {fileName}</div>}
          </div>

          <div className="jd-textarea-wrap">
            <textarea
              id="jd"
              className="jd-textarea"
              placeholder="Dán toàn bộ mô tả công việc vào đây. Hệ thống sẽ nhận diện chủ đề kỹ thuật và cấp độ phù hợp."
              value={rawText}
              onChange={(event) => setRawText(event.target.value.slice(0, 5000))}
              disabled={loading}
              maxLength={5000}
            />
            <span className="jd-counter">{rawText.length}/5000</span>
          </div>

          <div className="quick-fill" aria-label="Mẫu mô tả công việc nhanh">
            {Object.entries(QUICK_SAMPLES).map(([label, sample]) => (
              <button key={label} type="button" className="quick-chip" onClick={() => setRawText(sample.slice(0, 5000))} disabled={loading}>{label}</button>
            ))}
          </div>

          <button type="button" className="analyze-button" onClick={handleAnalyze} disabled={loading} aria-busy={loading}>
            {loading ? <><span className="spinner" />Đang phân tích JD…</> : 'Phân tích JD ✨'}
          </button>
        </div>
      )}

      {step === 'config' && analysis && (
        <div style={s.card}>
          <h2 style={{ ...s.sectionTitle, fontSize: '1.05rem' }}>Cấu hình phỏng vấn</h2>
          <p style={s.mutedText}>AI đã nhận diện các chủ đề từ JD. Bạn có thể bỏ chọn chủ đề không muốn hỏi.</p>

          <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap', marginTop: '0.75rem' }}>
            <div style={s.field}>
              <label style={s.label} htmlFor="level">Cấp độ</label>
              <select id="level" style={{ ...s.select, maxWidth: 220 }} value={level} onChange={(e) => setLevel(e.target.value as Level)}>
                {LEVELS.map((l) => <option key={l} value={l}>{LEVEL_LABELS[l]}</option>)}
              </select>
            </div>
            <div style={s.field}>
              <label style={s.label} htmlFor="time">Thời lượng</label>
              <select id="time" style={{ ...s.select, maxWidth: 220 }} value={timeLimitSec} onChange={(e) => setTimeLimitSec(Number(e.target.value))}>
                {TIME_OPTIONS.map((t) => <option key={t.sec} value={t.sec}>{t.label}</option>)}
              </select>
            </div>
          </div>

          <div style={{ ...s.field, marginTop: '0.75rem' }}>
            <span style={s.label}>Chủ đề phỏng vấn</span>
            <div style={{ ...s.chipList, flexDirection: 'column', alignItems: 'stretch', gap: '0.5rem', marginTop: '0.5rem' }}>
              {analysis.terms.length === 0 && <span style={s.mutedText}>Không nhận diện được chủ đề nào.</span>}
              {analysis.terms.map((t) => {
                const checked = t.topicId ? !!selected[t.topicId] : false
                return (
                  <label key={t.jdTopicId} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <input
                      type="checkbox"
                      checked={checked}
                      disabled={!t.topicId}
                      onChange={() => {
                        if (!t.topicId) return
                        setSelected((prev) => ({ ...prev, [t.topicId as string]: !prev[t.topicId as string] }))
                      }}
                    />
                    <span>{t.rawTerm}</span>
                    {t.topicCode && <span style={s.chip}>{t.topicCode}</span>}
                    {!t.topicId && <span style={{ ...s.badge, ...s.badgeWarn }}>chưa hỗ trợ</span>}
                  </label>
                )
              })}
            </div>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
            <button type="button" style={{ ...s.primaryButton, ...(loading ? s.disabledButton : {}) }} onClick={handleStart} disabled={loading}>
              {loading ? 'Đang tạo…' : 'Bắt đầu phỏng vấn'}
            </button>
            <button type="button" style={s.secondaryButton} onClick={() => setStep('input')} disabled={loading}>
              Quay lại
            </button>
          </div>
        </div>
      )}
    </>
  )
}
