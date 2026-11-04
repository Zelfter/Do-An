/**
 * AppRouter — React Router v6 routing shell.
 *
 * Routes:
 *   /            → redirect to /login
 *   /login       → LoginPage (public)
 *   /admin       → AdminLayout (private, ADMIN role required)
 *
 * PrivateRoute enforces:
 *   1. User must be authenticated → else redirect to /login
 *   2. User must have role ADMIN  → else redirect to /login
 *
 * While the AuthContext is still loading (resolving existing token),
 * a loading indicator is shown to avoid a flash-redirect.
 *
 * AC-RBAC-01: Candidate JWT → /admin → redirect to /login
 */
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import LoginPage from '../pages/LoginPage'
import RegisterPage from '../pages/RegisterPage'
import AdminLayout from '../pages/admin/AdminLayout'
import TopicsPage from '../pages/admin/TopicsPage'
import QuestionsPage from '../pages/admin/QuestionsPage'
import DocumentsPage from '../pages/admin/DocumentsPage'
import UsersPage from '../pages/admin/UsersPage'
import CandidateLayout from '../pages/candidate/CandidateLayout'
import CandidateDashboard from '../pages/candidate/CandidateDashboard'
import NewInterviewPage from '../pages/candidate/NewInterviewPage'
import InterviewReportPage from '../pages/candidate/InterviewReportPage'
import InterviewRoomPage from '../pages/interview/InterviewRoomPage'

// ─── Route guard ──────────────────────────────────────────────────────────────

export function PrivateRoute({ requiredRole }: { requiredRole?: 'ADMIN' | 'CANDIDATE' }) {
  const { user, loading } = useAuth()

  if (loading) {
    return <div aria-live="polite">Đang tải…</div>
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (requiredRole && user.role !== requiredRole) {
    // CANDIDATE trying to access /admin → redirect (AC-RBAC-01)
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}

/** Redirects root/404 by auth state: admin → /admin, candidate → /dashboard, else /login. */
function HomeRedirect() {
  const { user, loading } = useAuth()
  if (loading) {
    return <div aria-live="polite">Đang tải…</div>
  }
  if (!user) {
    return <Navigate to="/login" replace />
  }
  return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} replace />
}

// ─── Route configuration (separated for testing with MemoryRouter) ────────────

export function AppRoutes() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Admin routes — require ADMIN role */}
      <Route element={<PrivateRoute requiredRole="ADMIN" />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="/admin/topics" replace />} />
          <Route path="topics" element={<TopicsPage />} />
          <Route path="questions" element={<QuestionsPage />} />
          <Route path="documents" element={<DocumentsPage />} />
          <Route path="users" element={<UsersPage />} />
        </Route>
      </Route>

      {/* Candidate routes — require CANDIDATE role */}
      <Route element={<PrivateRoute requiredRole="CANDIDATE" />}>
        <Route path="/dashboard" element={<CandidateLayout><CandidateDashboard /></CandidateLayout>} />
        <Route path="/interview/new" element={<CandidateLayout><NewInterviewPage /></CandidateLayout>} />
        {/* The room is full-screen (its own header) — not wrapped in the layout */}
        <Route path="/interview/:sessionId" element={<InterviewRoomPage />} />
        <Route path="/interview/:sessionId/report" element={<CandidateLayout><InterviewReportPage /></CandidateLayout>} />
      </Route>

      {/* Default + 404: redirect by role (logged in → home, else login) */}
      <Route path="/" element={<HomeRedirect />} />
      <Route path="*" element={<HomeRedirect />} />
    </Routes>
  )
}

// ─── Router ───────────────────────────────────────────────────────────────────

export default function AppRouter() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}
