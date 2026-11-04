/**
 * CandidateLayout — shell for candidate-facing pages (Step 4).
 *
 * Mirrors AdminLayout but uses the lighter candidate theme (interviewStyles).
 * Renders its children directly (no <Outlet/>) so each route can wrap a page.
 */
import React from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { interviewStyles as s } from '../../components/shared/interviewStyles'

const ROLE_LABELS: Record<string, string> = { ADMIN: 'Quản trị', CANDIDATE: 'Ứng viên' }

export default function CandidateLayout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div style={s.page}>
      <header style={s.header}>
        <span style={s.brand}>Phỏng vấn mô phỏng</span>
        <div style={s.headerNav}>
          {user && (
            <span style={s.userInfo}>
              {user.email} ({ROLE_LABELS[user.role] ?? user.role})
            </span>
          )}
          <button type="button" style={s.ghostButtonLight} onClick={handleLogout}>
            Đăng xuất
          </button>
        </div>
      </header>

      <nav style={s.nav} aria-label="Điều hướng ứng viên">
        <NavLink
          to="/dashboard"
          style={({ isActive }) => ({ ...s.navLink, ...(isActive ? s.navLinkActive : {}) })}
        >
          Trang chủ
        </NavLink>
        <NavLink
          to="/interview/new"
          style={({ isActive }) => ({ ...s.navLink, ...(isActive ? s.navLinkActive : {}) })}
        >
          Tạo phỏng vấn
        </NavLink>
      </nav>

      <main style={s.main}>{children}</main>
    </div>
  )
}
