/**
 * AuthContext — global authentication state (design §2.2)
 *
 * Provides: current user, login(), logout().
 * On mount: attempts to restore user from an existing sessionStorage token
 * via GET /api/v1/me. If the token is absent or expired the user stays null.
 */
import React, { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { getMe, login as apiLogin, logout as apiLogout, tokenStorage, UserDto } from '../api/authApi'

// ─── Context shape ────────────────────────────────────────────────────────────

interface AuthContextValue {
  /** null while loading OR when not authenticated */
  user: UserDto | null
  /** true during the initial token→user resolution on mount */
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

// ─── Provider ─────────────────────────────────────────────────────────────────

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(null)
  const [loading, setLoading] = useState(true)

  // Restore session on mount
  useEffect(() => {
    let cancelled = false
    if (tokenStorage.getAccessToken()) {
      getMe()
        .then((u) => { if (!cancelled) setUser(u) })
        .catch(() => {
          // Token invalid / expired — clear storage silently
          tokenStorage.clear()
        })
        .finally(() => { if (!cancelled) setLoading(false) })
    } else {
      setLoading(false)
    }
    return () => { cancelled = true }
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    await apiLogin(email, password)      // stores tokens in sessionStorage
    const me = await getMe()
    setUser(me)
  }, [])

  const logout = useCallback(async () => {
    await apiLogout()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

// ─── Hook ─────────────────────────────────────────────────────────────────────

/** Must be used inside <AuthProvider>. */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
