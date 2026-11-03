/**
 * Auth API client — design §2.2
 *
 * Axios instance proxied through Vite → Java backend :8080.
 * Frontend NEVER calls the Python AI service directly. (architecture-guardrails.md)
 *
 * Token storage: sessionStorage (clears on tab close; preferred over localStorage
 * for sensitive sessions).
 */
import axios from 'axios'

// ─── Types ────────────────────────────────────────────────────────────────────

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface UserDto {
  id: string
  email: string
  role: 'CANDIDATE' | 'ADMIN'
  status: 'ACTIVE' | 'LOCKED'
}

// ─── Storage helpers ──────────────────────────────────────────────────────────

const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

export const tokenStorage = {
  getAccessToken: (): string | null => sessionStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: (): string | null => sessionStorage.getItem(REFRESH_TOKEN_KEY),
  setTokens: (accessToken: string, refreshToken: string): void => {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
    sessionStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  },
  clear: (): void => {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY)
    sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  },
}

// ─── Axios instance ───────────────────────────────────────────────────────────

export const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// Inject Bearer token on every request if present
apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ─── Auth operations ──────────────────────────────────────────────────────────

/**
 * POST /api/v1/auth/login
 * Stores tokens in sessionStorage on success.
 */
export async function login(email: string, password: string): Promise<LoginResponse> {
  const { data } = await apiClient.post<LoginResponse>('/auth/login', { email, password })
  tokenStorage.setTokens(data.accessToken, data.refreshToken)
  return data
}

/**
 * POST /api/v1/auth/logout
 * Revokes the refresh token server-side, then clears local storage.
 */
export async function logout(): Promise<void> {
  const refreshToken = tokenStorage.getRefreshToken()
  if (refreshToken) {
    try {
      await apiClient.post('/auth/logout', { refreshToken })
    } catch {
      // Best-effort: clear locally even if server call fails
    }
  }
  tokenStorage.clear()
}

/**
 * GET /api/v1/me
 * Returns the current user; throws if token is missing/invalid.
 */
export async function getMe(): Promise<UserDto> {
  const { data } = await apiClient.get<UserDto>('/me')
  return data
}
