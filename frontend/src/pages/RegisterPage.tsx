/**
 * RegisterPage — isolated signup screen (append-only: new file).
 * NFR-26: all UI texts in Vietnamese.
 *
 * Form fields: Email, Password, Confirm Password.
 *
 * NOTE on the missing "Name" field:
 * The existing `users` table (Flyway V001) has NO "name" column, and the
 * append-only / no-schema-change constraint forbids adding one. A Name field is
 * therefore intentionally NOT collected. If names are needed later, a new
 * Flyway migration + entity update are required (that would relax the
 * constraint and is out of scope here).
 *
 * On success: shows a success message, then redirects to /login.
 */
import { FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../api/registerApi'
import { IconEye, IconEyeOff } from '../components/ui/icons'
import '../styles/auth.css'

interface FieldErrors {
  email?: string
  password?: string
  confirmPassword?: string
}

export default function RegisterPage() {
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  // ─── Client-side validation (before calling the API) ────────────────────────

  function validate(): boolean {
    const errors: FieldErrors = {}
    // Simple but practical RFC-ish check; the backend also validates with @Email.
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

    if (!email.trim()) {
      errors.email = 'Email không được để trống.'
    } else if (!emailRegex.test(email)) {
      errors.email = 'Email không đúng định dạng.'
    }

    if (!password) {
      errors.password = 'Mật khẩu không được để trống.'
    } else if (password.length < 8) {
      errors.password = 'Mật khẩu phải có ít nhất 8 ký tự.'
    }

    if (!confirmPassword) {
      errors.confirmPassword = 'Vui lòng xác nhận mật khẩu.'
    } else if (password !== confirmPassword) {
      errors.confirmPassword = 'Mật khẩu xác nhận không khớp.'
    }

    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  // ─── Submit ─────────────────────────────────────────────────────────────────

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setSubmitError(null)
    if (!validate()) return

    setSubmitting(true)
    try {
      await register(email, password)
      setSuccess(true)
      // Show the success message briefly, then redirect to login.
      setTimeout(() => navigate('/login', { replace: true }), 1500)
    } catch (err: unknown) {
      const status = getStatus(err)
      if (status === 409) {
        setSubmitError('Email đã tồn tại trong hệ thống.')
      } else if (status === 400) {
        setSubmitError('Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.')
      } else {
        setSubmitError('Có lỗi xảy ra. Vui lòng thử lại.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  // ─── Success view ───────────────────────────────────────────────────────────

  if (success) {
    return (
      <main className="auth-page">
        <section className="auth-panel">
          <div className="auth-card auth-success-card">
            <div className="auth-success-icon">✓</div>
            <h1 className="auth-title">Đăng ký thành công</h1>
            <p className="auth-subtitle" role="status" aria-live="polite">
              Tài khoản đã được tạo. Đang chuyển hướng về trang đăng nhập…
            </p>
            <Link
              className="auth-submit"
              style={{ display: 'grid', placeItems: 'center', textDecoration: 'none' }}
              to="/login"
            >
              Đến trang đăng nhập
            </Link>
          </div>
        </section>
        <aside className="auth-aside">
          <div className="auth-aside-content">
            <span className="auth-kicker">● Bắt đầu luyện tập</span>
            <h2>Mỗi buổi phỏng vấn là một bước tiến.</h2>
            <p>
              Tạo tài khoản để lưu lịch sử, nhận kết quả và luyện tập theo cấp độ của riêng bạn.
            </p>
          </div>
        </aside>
      </main>
    )
  }

  // ─── Form view ─────────────────────────────────────────────────────────────

  return (
    <main className="auth-page">
      <section className="auth-panel">
        <div className="auth-card">
          <div className="auth-brand">
            <span className="auth-brand-mark">AI</span> Interview Studio
          </div>
          <h1 className="auth-title">Tạo tài khoản</h1>
          <p className="auth-subtitle">Chỉ vài bước để bắt đầu luyện phỏng vấn mô phỏng theo JD.</p>

          <form onSubmit={handleSubmit} noValidate>
            <div className="auth-field">
              <label className="auth-label" htmlFor="email">
                Email
              </label>
              <input
                className="auth-input"
                id="email"
                type="email"
                autoComplete="email"
                placeholder="ten@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                aria-invalid={!!fieldErrors.email}
                aria-required="true"
              />
              {fieldErrors.email && <span role="alert" className="auth-error">{fieldErrors.email}</span>}
            </div>

            <div className="auth-field">
              <label className="auth-label" htmlFor="password">
                Mật khẩu
              </label>
              <div className="auth-input-wrap">
                <input
                  className="auth-input"
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  placeholder="Tối thiểu 8 ký tự"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  aria-invalid={!!fieldErrors.password}
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
              {fieldErrors.password && <span role="alert" className="auth-error">{fieldErrors.password}</span>}
            </div>

            <div className="auth-field">
              <label className="auth-label" htmlFor="confirmPassword">
                Xác nhận mật khẩu
              </label>
              <div className="auth-input-wrap">
                <input
                  className="auth-input"
                  id="confirmPassword"
                  type={showConfirmPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  placeholder="Nhập lại mật khẩu"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  aria-invalid={!!fieldErrors.confirmPassword}
                  aria-required="true"
                />
                <button
                  className="password-toggle"
                  type="button"
                  onClick={() => setShowConfirmPassword((prev) => !prev)}
                  aria-label={showConfirmPassword ? 'Ẩn nội dung xác nhận' : 'Hiện nội dung xác nhận'}
                >
                  {showConfirmPassword ? <IconEyeOff /> : <IconEye />}
                </button>
              </div>
              {fieldErrors.confirmPassword && <span role="alert" className="auth-error">{fieldErrors.confirmPassword}</span>}
            </div>

            {submitError && <p role="alert" aria-live="assertive" className="auth-alert">{submitError}</p>}
            <button className="auth-submit" type="submit" disabled={submitting} aria-busy={submitting}>
              {submitting ? 'Đang xử lý…' : 'Tạo tài khoản'}
            </button>
          </form>

          <p className="auth-footer">
            Đã có tài khoản?{' '}
            <Link className="auth-link" to="/login">
              Đăng nhập
            </Link>
          </p>
        </div>
      </section>

      <aside className="auth-aside">
        <div className="auth-aside-content">
          <span className="auth-kicker">● Phỏng vấn thông minh hơn</span>
          <h2>Chuẩn bị tốt cho cơ hội tiếp theo.</h2>
          <p>
            Hệ thống phân tích mô tả công việc, tạo câu hỏi phù hợp và giúp bạn nhìn rõ điểm mạnh cũng như điểm cần cải thiện.
          </p>
          <div className="auth-benefits">
            <div className="auth-benefit">
              <span className="auth-benefit-check">✓</span> Thiết lập câu hỏi theo chủ đề
            </div>
            <div className="auth-benefit">
              <span className="auth-benefit-check">✓</span> Theo dõi lịch sử luyện tập
            </div>
            <div className="auth-benefit">
              <span className="auth-benefit-check">✓</span> Trải nghiệm thân thiện trên mọi thiết bị
            </div>
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
