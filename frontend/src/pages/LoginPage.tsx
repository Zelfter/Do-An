/**
 * LoginPage — NFR-26: all UI text in Vietnamese.
 * AC-AUTH-03/04/07: handles 200, 401, 403 responses.
 */
import { FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { IconEye, IconEyeOff } from '../components/ui/icons'
import '../styles/auth.css'

export default function LoginPage() {
  const { login, user } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  // Becomes true only after a successful login submit; used to navigate by role.
  const [submitted, setSubmitted] = useState(false)

  // After a successful login the AuthContext resolves the user; redirect by role.
  useEffect(() => {
    if (submitted && user) {
      navigate(user.role === 'ADMIN' ? '/admin' : '/dashboard', { replace: true })
    }
  }, [submitted, user, navigate])

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setErrorMsg(null)
    setSubmitting(true)

    try {
      await login(email, password)
      setSubmitted(true)
    } catch (err: unknown) {
      // Map HTTP status codes to Vietnamese messages (AC-AUTH-04, AC-AUTH-07)
      const status = getStatus(err)
      if (status === 401) {
        setErrorMsg('Email hoặc mật khẩu không đúng.')
      } else if (status === 403) {
        setErrorMsg('Tài khoản bị khóa. Vui lòng liên hệ quản trị viên.')
      } else {
        setErrorMsg('Có lỗi xảy ra. Vui lòng thử lại.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-panel">
        <div className="auth-card">
          <div className="auth-brand"><span className="auth-brand-mark">AI</span> Interview Studio</div>
          <h1 className="auth-title">Chào mừng trở lại</h1>
          <p className="auth-subtitle">Đăng nhập để tiếp tục hành trình luyện phỏng vấn của bạn.</p>

          <form onSubmit={handleSubmit} noValidate>
            <div className="auth-field">
              <label className="auth-label" htmlFor="email">Email</label>
              <input
                className="auth-input"
                id="email"
                type="email"
                autoComplete="email"
                placeholder="ten@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                aria-required="true"
              />
            </div>

            <div className="auth-field">
              <label className="auth-label" htmlFor="password">Mật khẩu</label>
              <div className="auth-input-wrap">
                <input
                  className="auth-input"
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder="Nhập mật khẩu"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  aria-required="true"
                />
                <button
                  className="password-toggle"
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  aria-label={showPassword ? 'Ẩn nội dung' : 'Hiện nội dung'}
                >
                  {showPassword ? <IconEyeOff /> : <IconEye />}
                </button>
              </div>
            </div>

            {errorMsg && <p role="alert" aria-live="assertive" className="auth-alert">{errorMsg}</p>}

            <button className="auth-submit" type="submit" disabled={submitting} aria-busy={submitting}>
              {submitting ? 'Đang đăng nhập…' : 'Đăng nhập'}
            </button>
          </form>

          <p className="auth-footer">
            Chưa có tài khoản? <Link className="auth-link" to="/register">Tạo tài khoản mới</Link>
          </p>
        </div>
      </section>

      <aside className="auth-aside">
        <div className="auth-aside-content">
          <span className="auth-kicker">● Hệ thống phỏng vấn AI</span>
          <h2>Luyện tập tự tin. Trả lời nổi bật.</h2>
          <p>Dựng phòng phỏng vấn theo mô tả công việc, nhận phản hồi chi tiết và theo dõi tiến bộ của bạn theo từng chủ đề.</p>
          <div className="auth-benefits">
            <div className="auth-benefit"><span className="auth-benefit-check">✓</span> Câu hỏi cá nhân hóa theo JD</div>
            <div className="auth-benefit"><span className="auth-benefit-check">✓</span> Đánh giá kỹ năng và điểm số minh bạch</div>
            <div className="auth-benefit"><span className="auth-benefit-check">✓</span> Lịch sử luyện tập và báo cáo kết quả</div>
          </div>
        </div>
      </aside>
    </main>
  )
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function getStatus(err: unknown): number | null {
  if (
    err !== null &&
    typeof err === 'object' &&
    'response' in err &&
    err.response !== null &&
    typeof err.response === 'object' &&
    'status' in err.response
  ) {
    return (err.response as { status: number }).status
  }
  return null
}
