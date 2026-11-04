/**
 * Register API client — isolated, self-contained (append-only: new file).
 *
 * Calls the Java backend only (architecture-guardrails.md: frontend never talks
 * to the Python AI service or LLM directly). Uses its own axios instance so it
 * does not modify or depend on the existing {@code authApi.ts}.
 *
 * Endpoint: POST /api/v1/auth/signup (proxied to :8080 via vite.config.ts).
 */
import axios from 'axios'

// ─── Types ────────────────────────────────────────────────────────────────────

export interface RegisterResponse {
  id: string
  email: string
  role: 'CANDIDATE' | 'ADMIN'
  status: 'ACTIVE' | 'LOCKED'
}

// ─── Axios instance (self-contained, no dependency on existing authApi.ts) ────

const registerClient = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// ─── Operations ──────────────────────────────────────────────────────────────

/**
 * POST /api/v1/auth/signup
 *
 * @returns 201 {@link RegisterResponse} on success.
 * @throws  409 if the email already exists; 400 on validation failure.
 *          Callers map the error by inspecting `err.response.status`.
 */
export async function register(email: string, password: string): Promise<RegisterResponse> {
  const { data } = await registerClient.post<RegisterResponse>('/auth/signup', {
    email,
    password,
  })
  return data
}
