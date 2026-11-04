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
import React, { FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../api/registerApi'

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
      <main style={styles.wrapper}>
        <div style={styles.card}>
          <h1 style={styles.title}>Đăng ký thành công</h1>
          <p style={styles.successText} role="status" aria-live="polite">
            Tài khoản đã được tạo. Đang chuyển hướng về trang đăng nhập…
          </p>
          <Link to="/login" style={styles.link}>
            Đến trang đăng nhập
          </Link>
        </div>
      </main>
    )
  }

  // ─── Form view ─────────────────────────────────────────────────────────────

  return (
    <main style={styles.wrapper}>
      <div style={styles.card}>
        <h1 style={styles.title}>Đăng ký tài khoản</h1>
        <p style={styles.subtitle}>Tạo tài khoản để sử dụng hệ thống phỏng vấn mô phỏng</p>

        <form onSubmit={handleSubmit} noValidate>
          <div style={styles.field}>
            <label htmlFor="email" style={styles.label}>
              Email
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              aria-invalid={!!fieldErrors.email}
              aria-required="true"
              style={styles.input}
            />
            {fieldErrors.email && (
              <span role="alert" style={styles.fieldError}>
                {fieldErrors.email}
              </span>
            )}
          </div>

          <div style={styles.field}>
            <label htmlFor="password" style={styles.label}>
              Mật khẩu
            </label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              placeholder="Mật khẩu (tối thiểu 8 ký tự)"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              aria-invalid={!!fieldErrors.password}
              aria-required="true"
              style={styles.input}
            />
            {fieldErrors.password && (
              <span role="alert" style={styles.fieldError}>
                {fieldErrors.password}
              </span>
            )}
          </div>

          <div style={styles.field}>
            <label htmlFor="confirmPassword" style={styles.label}>
              Xác nhận mật khẩu
            </label>
            <input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              placeholder="Nhập lại mật khẩu"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              aria-invalid={!!fieldErrors.confirmPassword}
              aria-required="true"
              style={styles.input}
            />
            {fieldErrors.confirmPassword && (
              <span role="alert" style={styles.fieldError}>
                {fieldErrors.confirmPassword}
              </span>
            )}
          </div>

          {submitError && (
            <p role="alert" aria-live="assertive" style={styles.error}>
              {submitError}
            </p>
          )}

          <button type="submit" disabled={submitting} aria-busy={submitting} style={styles.button}>
            {submitting ? 'Đang xử lý…' : 'Đăng ký'}
          </button>
        </form>

        <p style={styles.footerText}>
          Đã có tài khoản?{' '}
          <Link to="/login" style={styles.link}>
            Đăng nhập
          </Link>
        </p>
      </div>
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

// ─── Minimal inline styles (mirrors LoginPage for consistency) ───────────────

const styles: Record<string, React.CSSProperties> = {
  wrapper: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    background: '#f5f5f5',
  },
  card: {
    background: '#fff',
    borderRadius: 8,
    padding: '2rem 2.5rem',
    boxShadow: '0 2px 12px rgba(0,0,0,0.1)',
    width: '100%',
    maxWidth: 400,
  },
  title: { margin: '0 0 0.25rem', fontSize: '1.5rem' },
  subtitle: { margin: '0 0 1.5rem', color: '#666', fontSize: '0.875rem' },
  field: { marginBottom: '1rem' },
  label: { display: 'block', marginBottom: 4, fontWeight: 500 },
  input: {
    width: '100%',
    padding: '0.5rem 0.75rem',
    border: '1px solid #ccc',
    borderRadius: 4,
    fontSize: '1rem',
    boxSizing: 'border-box',
  },
  fieldError: {
    display: 'block',
    color: '#c0392b',
    marginTop: 4,
    fontSize: '0.8125rem',
  },
  error: {
    color: '#c0392b',
    marginBottom: '0.75rem',
    fontSize: '0.875rem',
  },
  successText: {
    margin: '0 0 1.25rem',
    color: '#1a7f37',
    fontSize: '0.9375rem',
  },
  button: {
    width: '100%',
    padding: '0.625rem',
    background: '#1677ff',
    color: '#fff',
    border: 'none',
    borderRadius: 4,
    fontSize: '1rem',
    cursor: 'pointer',
  },
  footerText: {
    marginTop: '1.25rem',
    textAlign: 'center',
    fontSize: '0.875rem',
    color: '#555',
  },
  link: {
    color: '#1677ff',
    textDecoration: 'none',
  },
}
