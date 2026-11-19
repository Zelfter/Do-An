/**
 * Topbar — admin header: mobile menu toggle, decorative search, notifications
 * and an inline profile (avatar + email + role + logout).
 * Presentational only; logout handler comes from the layout.
 */
import { IconMenu, IconSearch, IconBell, IconLogout } from './icons'

interface TopbarProps {
  title?: string
  onMenu: () => void
  user: { email: string; roleLabel: string }
  onLogout: () => void
}

function initials(email: string): string {
  const name = email.split('@')[0]
  return name.slice(0, 2).toUpperCase()
}

export default function Topbar({ title, onMenu, user, onLogout }: TopbarProps) {
  return (
    <header className="app-topbar">
      <button className="btn btn-ghost btn-icon menu-btn" onClick={onMenu} aria-label="Mở menu" type="button">
        <IconMenu />
      </button>

      {title && <span className="topbar-title">{title}</span>}

      <div className="topbar-search">
        <IconSearch size={16} />
        <input type="text" placeholder="Tìm kiếm…" aria-label="Tìm kiếm" />
      </div>

      <div className="topbar-spacer" />

      <button className="btn btn-ghost btn-icon" type="button" aria-label="Thông báo" style={{ position: 'relative' }}>
        <IconBell />
        <span
          style={{
            position: 'absolute', top: 6, right: 6, width: 8, height: 8, borderRadius: '50%',
            background: 'var(--danger)', border: '2px solid var(--surface)',
          }}
        />
      </button>

      <div className="topbar-profile">
        <div className="avatar" aria-hidden="true">{initials(user.email)}</div>
        <div className="profile-text">
          <div className="profile-name">{user.email}</div>
          <div className="profile-role">{user.roleLabel}</div>
        </div>
        <button className="btn btn-ghost btn-icon" onClick={onLogout} type="button" aria-label="Đăng xuất">
          <IconLogout />
        </button>
      </div>
    </header>
  )
}
