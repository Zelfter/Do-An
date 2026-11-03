/**
 * User management API client — design §2.3, FR-34.
 *
 * All calls use the shared apiClient (Bearer token interceptor already configured).
 */
import { apiClient, type UserDto } from './authApi'

// Re-export so callers can import everything user-related from one module.
export type { UserDto }

// ─── List / detail ───────────────────────────────────────────────────────────

/** GET /admin/users → 200 [{id, email, role, status}] (optional role/status filters) */
export async function listUsers(role?: string, status?: string): Promise<UserDto[]> {
  const params: Record<string, string> = {}
  if (role) params.role = role
  if (status) params.status = status
  const { data } = await apiClient.get<UserDto[]>('/admin/users', { params })
  return data
}

/** GET /admin/users/{id} → 200 | 404 */
export async function getUser(id: string): Promise<UserDto> {
  const { data } = await apiClient.get<UserDto>(`/admin/users/${id}`)
  return data
}

// ─── Status update ───────────────────────────────────────────────────────────

/** PATCH /admin/users/{id}/status → 200 | 400 (self-lock) | 404 */
export async function updateUserStatus(id: string, status: 'ACTIVE' | 'LOCKED'): Promise<UserDto> {
  const { data } = await apiClient.patch<UserDto>(`/admin/users/${id}/status`, { status })
  return data
}
