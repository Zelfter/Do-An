/**
 * AdminLayout — Admin section shell (redesigned, Step 2).
 *
 * Layout: collapsible sidebar (desktop) / drawer (mobile) + topbar + content + footer.
 *
 * LOGIC PRESERVED from the original: useAuth() user/logout, useNavigate,
 * handleLogout (logout → /login), the NAV_ITEMS set and ROLE_LABELS. Only the
 * JSX structure and styling changed; child routes still render via <Outlet/>.
 */
import { useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import Sidebar from '../../components/ui/Sidebar'
import Topbar from '../../components/ui/Topbar'
import Footer from '../../components/ui/Footer'
import { IconBook, IconHelpCircle, IconFileText, IconUsers } from '../../components/ui/icons'

const NAV_ITEMS = [
  { to: '/admin/topics', label: 'Chủ đề', icon: <IconBook /> },
  { to: '/admin/questions', label: 'Ngân hàng câu hỏi', icon: <IconHelpCircle /> },
  { to: '/admin/documents', label: 'Tài liệu', icon: <IconFileText /> },
  { to: '/admin/users', label: 'Người dùng', icon: <IconUsers /> },
]

// NFR-26: Vietnamese labels for enum values displayed to users.
const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Quản trị',
  CANDIDATE: 'Ứng viên',
}

export default function AdminLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  // UI-only state for the collapsible sidebar and the mobile drawer.
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app-shell" data-collapsed={collapsed} data-drawer-open={mobileOpen}>
      <Sidebar
        brand="Quản trị hệ thống"
        navItems={NAV_ITEMS}
        onToggleCollapse={() => setCollapsed((c) => !c)}
        mobileOpen={mobileOpen}
        onCloseMobile={() => setMobileOpen(false)}
        footerNote="Hệ thống giả lập phỏng vấn AI"
      />

      <div className="app-main">
        <Topbar
          title="Bảng điều khiển"
          onMenu={() => setMobileOpen(true)}
          user={{ email: user?.email ?? '', roleLabel: ROLE_LABELS[user?.role ?? ''] ?? user?.role ?? '' }}
          onLogout={handleLogout}
        />

        {/* Child routes rendered here */}
        <main className="app-content">
          <Outlet />
        </main>

        <Footer />
      </div>

      <div className="app-dimmer" onClick={() => setMobileOpen(false)} aria-hidden="true" />
    </div>
  )
}
