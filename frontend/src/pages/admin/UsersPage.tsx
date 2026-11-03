/**
 * UsersPage — Admin user management (FR-34, NFR-26).
 *
 * Features:
 *  - List users (email, role, status) with optional filters
 *  - Lock / unlock users (PATCH status)
 *  - Self-lock protection (backend returns 400; UI guards the current admin)
 *
 * All UI text is Vietnamese (NFR-26).
 */
import { useEffect, useState } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { listUsers, updateUserStatus, type UserDto } from '../../api/userApi'
import { adminStyles } from '../../components/shared/adminStyles'

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Hoạt động',
  LOCKED: 'Bị khóa',
}
// NFR-26: Vietnamese labels for enum values displayed to users.
const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Quản trị',
  CANDIDATE: 'Ứng viên',
}

function getStatus(err: unknown): number | null {
  if (err && typeof err === 'object' && 'response' in err && err.response && typeof err.response === 'object' && 'status' in err.response) {
    return (err.response as { status: number }).status
  }
  return null
}

export default function UsersPage() {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState<UserDto[]>([])
  const [loading, setLoading] = useState(true)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)
  const [filterRole, setFilterRole] = useState('')
  const [filterStatus, setFilterStatus] = useState('')

  async function load() {
    setLoading(true)
    setErrorMsg(null)
    try {
      const data = await listUsers(filterRole || undefined, filterStatus || undefined)
      setUsers(data)
    } catch {
      setErrorMsg('Không thể tải danh sách người dùng.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [filterRole, filterStatus])

  async function handleToggleLock(target: UserDto) {
    setErrorMsg(null)
    setSuccessMsg(null)
    const newStatus = target.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE'
    try {
      const updated = await updateUserStatus(target.id, newStatus)
      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)))
      setSuccessMsg(newStatus === 'LOCKED' ? `Đã khóa tài khoản ${target.email}.` : `Đã mở khóa tài khoản ${target.email}.`)
    } catch (err) {
      const status = getStatus(err)
      if (status === 400) {
        setErrorMsg('Không thể khóa chính tài khoản của bạn.')
      } else if (status === 404) {
        setErrorMsg('Người dùng không tồn tại.')
      } else {
        setErrorMsg('Không thể cập nhật trạng thái người dùng.')
      }
    }
  }

  // ─── Render ─────────────────────────────────────────────────────────────────

  return (
    <div style={adminStyles.page}>
      <div style={adminStyles.pageHeader}>
        <h1 style={adminStyles.pageHeading}>Quản lý người dùng</h1>
      </div>

      {/* Filters */}
      <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem', alignItems: 'center' }}>
        <label style={{ fontSize: '0.9rem' }}>
          Vai trò:{' '}
          <select style={{ ...adminStyles.select, width: 'auto' }} value={filterRole} onChange={(e) => setFilterRole(e.target.value)}>
            <option value="">Tất cả</option>
            <option value="ADMIN">Quản trị</option>
            <option value="CANDIDATE">Ứng viên</option>
          </select>
        </label>
        <label style={{ fontSize: '0.9rem' }}>
          Trạng thái:{' '}
          <select style={{ ...adminStyles.select, width: 'auto' }} value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            <option value="">Tất cả</option>
            <option value="ACTIVE">Hoạt động</option>
            <option value="LOCKED">Bị khóa</option>
          </select>
        </label>
      </div>

      {errorMsg && <p role="alert" style={adminStyles.errorText}>{errorMsg}</p>}
      {successMsg && <p style={adminStyles.successText}>{successMsg}</p>}

      {loading ? (
        <p>Đang tải…</p>
      ) : users.length === 0 ? (
        <p>Chưa có người dùng nào.</p>
      ) : (
        <table style={adminStyles.table}>
          <thead>
            <tr>
              <th style={adminStyles.th}>Email</th>
              <th style={adminStyles.th}>Vai trò</th>
              <th style={adminStyles.th}>Trạng thái</th>
              <th style={adminStyles.th}>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const isSelf = currentUser?.id === u.id
              return (
                <tr key={u.id}>
                  <td style={adminStyles.td}>{u.email}{isSelf && <span style={{ color: '#888', fontSize: '0.8rem' }}> (bạn)</span>}</td>
                  <td style={adminStyles.td}>{ROLE_LABELS[u.role] ?? u.role}</td>
                  <td style={adminStyles.td}>
                    <span style={{ ...adminStyles.badge, ...(u.status === 'ACTIVE' ? adminStyles.badgeActive : adminStyles.badgeInactive) }}>
                      {STATUS_LABELS[u.status] ?? u.status}
                    </span>
                  </td>
                  <td style={adminStyles.td}>
                    <button
                      style={adminStyles.smallButton}
                      onClick={() => handleToggleLock(u)}
                      disabled={isSelf}
                      title={isSelf ? 'Không thể khóa chính mình' : undefined}
                    >
                      {u.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa'}
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </div>
  )
}
