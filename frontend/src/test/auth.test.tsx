/**
 * Auth UI tests — Vitest + React Testing Library
 *
 * Covers:
 * - LoginPage renders with expected fields and button
 * - Form submission calls the auth API (axios mocked)
 * - Successful login navigates to /admin
 * - 401 shows Vietnamese error message
 * - 403 shows locked-account message
 * - Unauthenticated access to /admin redirects to /login (PrivateRoute)
 * - Logout clears sessionStorage tokens
 */
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../api/authApi'
import type { UserDto } from '../api/authApi'
import { AuthProvider } from '../contexts/AuthContext'
import AdminLayout from '../pages/admin/AdminLayout'
import LoginPage from '../pages/LoginPage'
import { AppRoutes } from '../router/AppRouter'

// ─── Helpers ──────────────────────────────────────────────────────────────────

function renderLoginPage() {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/admin" element={<div>Admin home</div>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>
  )
}

// ─── Setup / Teardown ─────────────────────────────────────────────────────────

beforeEach(() => {
  sessionStorage.clear()
  vi.restoreAllMocks()
})

afterEach(() => {
  sessionStorage.clear()
})

// ─── LoginPage rendering ──────────────────────────────────────────────────────

describe('LoginPage — render', () => {
  it('renders email and password fields', () => {
    // getMe will throw because no token — that is fine, loading resolves quickly
    vi.spyOn(authApi, 'getMe').mockRejectedValue(new Error('no token'))

    renderLoginPage()

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/mật khẩu/i)).toBeInTheDocument()
  })

  it('renders a submit button labelled "Đăng nhập"', () => {
    vi.spyOn(authApi, 'getMe').mockRejectedValue(new Error('no token'))

    renderLoginPage()

    expect(screen.getByRole('button', { name: /đăng nhập/i })).toBeInTheDocument()
  })
})

// ─── LoginPage — form submission ──────────────────────────────────────────────

describe('LoginPage — form submission', () => {
  it('calls login API with email and password', async () => {
    vi.spyOn(authApi, 'getMe').mockRejectedValue(new Error('no token'))
    const loginSpy = vi.spyOn(authApi, 'login').mockResolvedValue({
      accessToken: 'tok',
      refreshToken: 'rtok',
      expiresIn: 3600,
    })
    // After login, getMe is called again to get the user
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 'u1',
      email: 'admin@test.com',
      role: 'ADMIN',
      status: 'ACTIVE',
    })

    renderLoginPage()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/email/i), 'admin@test.com')
    await user.type(screen.getByLabelText(/mật khẩu/i), 'secret123')
    await user.click(screen.getByRole('button', { name: /đăng nhập/i }))

    await waitFor(() => expect(loginSpy).toHaveBeenCalledWith('admin@test.com', 'secret123'))
  })

  it('navigates to /admin after successful login', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      accessToken: 'tok',
      refreshToken: 'rtok',
      expiresIn: 3600,
    })
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 'u1',
      email: 'admin@test.com',
      role: 'ADMIN',
      status: 'ACTIVE',
    })

    renderLoginPage()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/email/i), 'admin@test.com')
    await user.type(screen.getByLabelText(/mật khẩu/i), 'secret123')
    await user.click(screen.getByRole('button', { name: /đăng nhập/i }))

    await waitFor(() => expect(screen.getByText('Admin home')).toBeInTheDocument())
  })

  it('shows Vietnamese error on 401', async () => {
    vi.spyOn(authApi, 'getMe').mockRejectedValue(new Error('no token'))
    vi.spyOn(authApi, 'login').mockRejectedValue({ response: { status: 401 } })

    renderLoginPage()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/email/i), 'bad@test.com')
    await user.type(screen.getByLabelText(/mật khẩu/i), 'wrong')
    await user.click(screen.getByRole('button', { name: /đăng nhập/i }))

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(
        'Email hoặc mật khẩu không đúng.'
      )
    )
  })

  it('shows locked-account message on 403', async () => {
    vi.spyOn(authApi, 'getMe').mockRejectedValue(new Error('no token'))
    vi.spyOn(authApi, 'login').mockRejectedValue({ response: { status: 403 } })

    renderLoginPage()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/email/i), 'locked@test.com')
    await user.type(screen.getByLabelText(/mật khẩu/i), 'pass')
    await user.click(screen.getByRole('button', { name: /đăng nhập/i }))

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('Tài khoản bị khóa')
    )
  })
})

// ─── PrivateRoute guard ───────────────────────────────────────────────────────

describe('PrivateRoute', () => {
  it('redirects unauthenticated user from /admin to /login', async () => {
    // No token in sessionStorage → getMe not called, loading resolves immediately
    vi.spyOn(authApi, 'getMe').mockRejectedValue(new Error('no token'))

    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/admin']}>
          <AppRoutes />
        </MemoryRouter>
      </AuthProvider>
    )

    // Admin content should NOT be visible (PrivateRoute redirects to /login)
    await waitFor(() =>
      expect(screen.queryByText('Quản trị hệ thống')).not.toBeInTheDocument()
    )
    // Login form should appear instead
    expect(screen.getByRole('button', { name: /đăng nhập/i })).toBeInTheDocument()
  })

  it('redirects Candidate away from /admin to /login (AC-RBAC-01)', async () => {
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 'u1',
      email: 'candidate@test.com',
      role: 'CANDIDATE',
      status: 'ACTIVE',
    })
    authApi.tokenStorage.setTokens('access-tok', 'refresh-tok')

    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/admin']}>
          <AppRoutes />
        </MemoryRouter>
      </AuthProvider>
    )

    // Candidate should be redirected away from admin
    await waitFor(() =>
      expect(screen.queryByText('Quản trị hệ thống')).not.toBeInTheDocument()
    )
    expect(screen.getByRole('button', { name: /đăng nhập/i })).toBeInTheDocument()
  })

  it('allows authenticated Admin to access /admin', async () => {
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 'u1',
      email: 'admin@test.com',
      role: 'ADMIN',
      status: 'ACTIVE',
    })
    authApi.tokenStorage.setTokens('access-tok', 'refresh-tok')

    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/admin']}>
          <AppRoutes />
        </MemoryRouter>
      </AuthProvider>
    )

    await waitFor(() =>
      expect(screen.getByText('Quản trị hệ thống')).toBeInTheDocument()
    )
  })

  it('shows loading indicator (Vietnamese) while token is being resolved', () => {
    // A token exists so getMe() is called; keep the promise pending so
    // loading stays true during the assertion (no premature redirect).
    vi.spyOn(authApi, 'getMe').mockReturnValue(new Promise<UserDto>(() => {}))
    authApi.tokenStorage.setTokens('access-tok', 'refresh-tok')

    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/admin']}>
          <AppRoutes />
        </MemoryRouter>
      </AuthProvider>
    )

    // Loading indicator should be visible (NFR-26: Vietnamese text)
    expect(screen.getByText('Đang tải…')).toBeInTheDocument()
    // Admin content should NOT be visible during loading
    expect(screen.queryByText('Quản trị hệ thống')).not.toBeInTheDocument()
    // Login form should NOT be visible during loading (no premature redirect)
    expect(screen.queryByRole('button', { name: /đăng nhập/i })).not.toBeInTheDocument()
  })

  it('redirects to /login when token is invalid/expired', async () => {
    // Token exists but is rejected by the server → user becomes null
    vi.spyOn(authApi, 'getMe').mockRejectedValue(new Error('invalid token'))
    authApi.tokenStorage.setTokens('expired-tok', 'refresh-tok')

    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/admin']}>
          <AppRoutes />
        </MemoryRouter>
      </AuthProvider>
    )

    // After getMe rejects, loading=false + user=null → redirect to /login
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /đăng nhập/i })).toBeInTheDocument()
    )
    // Admin content should NOT be visible
    expect(screen.queryByText('Quản trị hệ thống')).not.toBeInTheDocument()
    // Invalid token should be cleared from storage
    expect(authApi.tokenStorage.getAccessToken()).toBeNull()
  })
})

// ─── Logout ───────────────────────────────────────────────────────────────────

describe('Logout', () => {
  it('clears sessionStorage tokens on logout', async () => {
    authApi.tokenStorage.setTokens('access-tok', 'refresh-tok')
    vi.spyOn(authApi, 'logout').mockImplementation(async () => {
      authApi.tokenStorage.clear()
    })

    await authApi.logout()

    expect(sessionStorage.getItem('access_token')).toBeNull()
    expect(sessionStorage.getItem('refresh_token')).toBeNull()
  })

  it('renders logout button in AdminLayout when user is authenticated', async () => {
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 'u1',
      email: 'admin@test.com',
      role: 'ADMIN',
      status: 'ACTIVE',
    })
    authApi.tokenStorage.setTokens('access-tok', 'refresh-tok')

    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/admin']}>
          <Routes>
            <Route path="/login" element={<div>Login page</div>} />
            <Route path="/admin" element={<AdminLayout />} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    )

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /đăng xuất/i })).toBeInTheDocument()
    )
  })

  it('clicking logout redirects to /login (DoD: logout clears tokens and redirects)', async () => {
    const logoutSpy = vi.spyOn(authApi, 'logout').mockImplementation(async () => {
      authApi.tokenStorage.clear()
    })
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 'u1',
      email: 'admin@test.com',
      role: 'ADMIN',
      status: 'ACTIVE',
    })
    authApi.tokenStorage.setTokens('access-tok', 'refresh-tok')

    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/admin']}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/admin" element={<AdminLayout />} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    )

    // Wait for AdminLayout to render with the logout button
    const logoutBtn = await screen.findByRole('button', { name: /đăng xuất/i })
    const user = userEvent.setup()
    await user.click(logoutBtn)

    // Logout should have been called
    await waitFor(() => expect(logoutSpy).toHaveBeenCalled())
    // Login page should appear after redirect
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /đăng nhập/i })).toBeInTheDocument()
    )
    // Tokens should be cleared
    expect(sessionStorage.getItem('access_token')).toBeNull()
    expect(sessionStorage.getItem('refresh_token')).toBeNull()
  })
})
