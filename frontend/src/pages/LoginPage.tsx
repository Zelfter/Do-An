/**
 * LoginPage — NFR-26: all UI text in Vietnamese.
 * AC-AUTH-03/04/07: handles 200, 401, 403 responses.
 */
import React, { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setErrorMsg(null)
    setSubmitting(true)

    try {
      await login(email, password)
      navigate('/admin', { replace: true })
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
    <main style={styles.wrapper}>
      <div style={styles.card}>
        <h1 style={styles.title}>Đăng nhập</h1>
        <p style={styles.subtitle}>Ôn Java &amp; Phỏng vấn mô phỏng theo JD</p>

        <form onSubmit={handleSubmit} noValidate>
          <div style={styles.field}>
            <label htmlFor="email" style={styles.label}>Email</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              aria-required="true"
              style={styles.input}
            />
          </div>

          <div style={styles.field}>
            <label htmlFor="password" style={styles.label}>Mật khẩu</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              placeholder="Mật khẩu"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              aria-required="true"
              style={styles.input}
            />
          </div>

          {errorMsg && (
            <p role="alert" aria-live="assertive" style={styles.error}>
              {errorMsg}
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            aria-busy={submitting}
            style={styles.button}
          >
            {submitting ? 'Đang đăng nhập…' : 'Đăng nhập'}
          </button>
        </form>
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

// ─── Minimal inline styles (no external CSS deps required for scaffold) ───────

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
  error: {
    color: '#c0392b',
    marginBottom: '0.75rem',
    fontSize: '0.875rem',
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
}
