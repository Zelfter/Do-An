/**
 * CandidateLayout — Candidate portal shell (redesigned, Step 2).
 *
 * Layout: sticky top navigation (brand + nav links + user) + content + footer.
 * On mobile the nav collapses into a toggle-down menu.
 *
 * LOGIC PRESERVED from the original: useAuth() user/logout, useNavigate,
 * handleLogout (logout → /login) and the navigation set. Only JSX/styling
 * changed; child pages render via {children}.
 */
import React, { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import Footer from '../../components/ui/Footer'
import { IconSparkles, IconLogout, IconMenu, IconClose } from '../../components/ui/icons'

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Quản trị',
  CANDIDATE: 'Ứng viên',
}

const NAV = [
  { to: '/dashboard', label: 'Trang chủ' },
  { to: '/interview/new', label: 'Tạo phỏng vấn' },
]

export default function CandidateLayout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="cand-shell" data-open={open}>
      <header className="cand-topbar">
        <NavLink className="cand-brand" to="/dashboard" aria-label="Trang chủ">
          <span style={{ display: 'grid', placeItems: 'center', width: 36, height: 36, borderRadius: 11, background: 'linear-gradient(135deg,#4f46e5,#7c3aed)', color: '#fff', boxShadow: '0 8px 18px rgba(79,70,229,.18)' }}>
            <IconSparkles size={19} />
          </span>
          <span>Phỏng vấn mô phỏng</span>
        </NavLink>

        <nav className="cand-nav" aria-label="Điều hướng ứng viên">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => 'cand-nav-link' + (isActive ? ' active' : '')}
              onClick={() => setOpen(false)}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="cand-user">
          <span className="cand-user-email">{user?.email}</span>
          {user && <span className="badge badge-brand">{ROLE_LABELS[user.role] ?? user.role}</span>}
          <button className="btn btn-ghost btn-icon" onClick={handleLogout} type="button" aria-label="Đăng xuất" title="Đăng xuất">
            <IconLogout />
          </button>
        </div>

        <button
          className="btn btn-ghost btn-icon cand-menu-btn"
          onClick={() => setOpen((value) => !value)}
          type="button"
          aria-label={open ? 'Đóng menu' : 'Mở menu'}
        >
          {open ? <IconClose /> : <IconMenu />}
        </button>
      </header>

      <main className="cand-content">{children}</main>
      <Footer />
    </div>
  )
}
