/**
 * AdminLayout — layout for the Admin section (Task 08).
 * Header + horizontal nav + logout button + Outlet for child routes.
 * NFR-26: all text in Vietnamese.
 * FR-RBAC: only ADMIN role reaches this route (enforced by PrivateRoute).
 */
import React from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'

const NAV_ITEMS = [
  { to: '/admin/topics', label: 'Chủ đề' },
  { to: '/admin/questions', label: 'Câu hỏi' },
  { to: '/admin/documents', label: 'Tài liệu' },
  { to: '/admin/users', label: 'Người dùng' },
]

// NFR-26: Vietnamese labels for enum values displayed to users.
const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Quản trị',
  CANDIDATE: 'Ứng viên',
}

export default function AdminLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div style={styles.wrapper}>
      <header style={styles.header}>
        <span style={styles.brand}>Quản trị hệ thống</span>
        <nav style={styles.nav}>
          {user && (
            <span style={styles.userInfo}>
              {user.email} ({ROLE_LABELS[user.role] ?? user.role})
            </span>
          )}
          <button onClick={handleLogout} style={styles.logoutBtn} type="button">
            Đăng xuất
          </button>
        </nav>
      </header>

      {/* Sub-navigation to CRUD pages (NFR-26: Vietnamese labels) */}
      <nav style={styles.subnav} aria-label="Điều hướng quản trị">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            style={({ isActive }) => ({
              ...styles.subnavLink,
              ...(isActive ? styles.subnavLinkActive : {}),
            })}
          >
            {item.label}
          </NavLink>
        ))}
      </nav>

      <main style={styles.main}>
        {/* Child routes rendered here */}
        <Outlet />
      </main>
    </div>
  )
}

// ─── Minimal inline styles ────────────────────────────────────────────────────

const styles: Record<string, React.CSSProperties> = {
  wrapper: { minHeight: '100vh', display: 'flex', flexDirection: 'column' },
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 1.5rem',
    height: 56,
    background: '#001529',
    color: '#fff',
  },
  brand: { fontWeight: 600, fontSize: '1rem' },
  nav: { display: 'flex', alignItems: 'center', gap: '1rem' },
  userInfo: { fontSize: '0.875rem', opacity: 0.8 },
  logoutBtn: {
    background: 'transparent',
    border: '1px solid rgba(255,255,255,0.4)',
    color: '#fff',
    padding: '0.25rem 0.75rem',
    borderRadius: 4,
    cursor: 'pointer',
    fontSize: '0.875rem',
  },
  subnav: {
    display: 'flex',
    gap: '0.5rem',
    padding: '0 1.5rem',
    background: '#fff',
    borderBottom: '1px solid #f0f0f0',
  },
  subnavLink: {
    padding: '0.75rem 1rem',
    color: '#595959',
    textDecoration: 'none',
    fontSize: '0.9rem',
    borderBottom: '2px solid transparent',
  },
  subnavLinkActive: {
    color: '#1677ff',
    borderBottomColor: '#1677ff',
    fontWeight: 600,
  },
  main: { flex: 1, padding: '1.5rem' },
}
