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
import { IconLock } from '../../components/ui/icons'

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

function initials(email: string): string {
  const name = email.split('@')[0]
  return name.slice(0, 2).toUpperCase()
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
        <div>
          <h1 style={adminStyles.pageHeading}>Quản lý người dùng</h1>
          <p className="muted" style={{ margin: '6px 0 0', fontSize: '.88rem' }}>Xem và quản lý trạng thái tài khoản trong hệ thống</p>
        </div>
      </div>

      <div className="users-toolbar">
        <div className="users-filters">
          <label className="filter-control">
            <span>Vai trò</span>
            <select className="input" value={filterRole} onChange={(e) => setFilterRole(e.target.value)}>
              <option value="">Tất cả</option>
              <option value="ADMIN">Quản trị</option>
              <option value="CANDIDATE">Ứng viên</option>
            </select>
          </label>
          <label className="filter-control">
            <span>Trạng thái</span>
            <select className="input" value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
              <option value="">Tất cả</option>
              <option value="ACTIVE">Hoạt động</option>
              <option value="LOCKED">Bị khóa</option>
            </select>
          </label>
        </div>
        {!loading && <span className="history-count">{users.length} người dùng</span>}
      </div>

      {errorMsg && <p role="alert" style={adminStyles.errorText}>{errorMsg}</p>}
      {successMsg && <p style={adminStyles.successText}>{successMsg}</p>}

      {loading ? (
        <div className="card"><p className="muted">Đang tải danh sách người dùng…</p></div>
      ) : users.length === 0 ? (
        <div className="card empty">Chưa có người dùng nào phù hợp với bộ lọc.</div>
      ) : (
        <div className="table-wrap users-table-card">
          <table className="table">
            <thead><tr><th>EMAIL</th><th>VAI TRÒ</th><th>TRẠNG THÁI</th><th>THAO TÁC</th></tr></thead>
            <tbody>
              {users.map((u) => {
                const isSelf = currentUser?.id === u.id
                return (
                  <tr key={u.id}>
                    <td>
                      <div className="user-email-cell">
                        <span className="user-avatar">{initials(u.email)}</span>
                        <span><span className="user-email-main">{u.email}</span>{isSelf && <span className="user-self">(bạn)</span>}</span>
                      </div>
                    </td>
                    <td><span className="badge badge-brand">{ROLE_LABELS[u.role] ?? u.role}</span></td>
                    <td><span className={'badge ' + (u.status === 'ACTIVE' ? 'badge-success' : 'badge-danger')}>{STATUS_LABELS[u.status] ?? u.status}</span></td>
                    <td>
                      <button
                        className={'btn btn-sm users-action ' + (u.status === 'ACTIVE' ? 'btn-danger' : 'btn-success')}
                        onClick={() => handleToggleLock(u)}
                        disabled={isSelf}
                        title={isSelf ? 'Không thể khóa chính mình' : undefined}
                      >
                        {u.status === 'ACTIVE' ? <><IconLock size={15} /> Khóa</> : 'Mở khóa'}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
