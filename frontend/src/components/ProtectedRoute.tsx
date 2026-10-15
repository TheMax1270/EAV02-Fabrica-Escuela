import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import type { AuthUser } from '../api/auth'
import { useAuth } from '../context/useAuth'
import ForbiddenPage from '../pages/ForbiddenPage'

type ProtectedRouteProps = {
  children: ReactNode
  role?: AuthUser['role']
}

export default function ProtectedRoute({ children, role }: ProtectedRouteProps) {
  const { user, isAuthenticated, isRestoringSession, sessionRestoreError, retrySessionRestore, logout } = useAuth()
  if (isRestoringSession) return <main className="auth-main"><p className="loading-note" role="status">Restaurando sesión...</p></main>
  if (sessionRestoreError && !isAuthenticated) return <main className="auth-main">
    <section className="auth-panel"><p className="form-error" role="alert">{sessionRestoreError}</p>
      <button className="button button-primary" type="button" onClick={() => void retrySessionRestore()}>Reintentar</button>
      <button className="button button-secondary" type="button" onClick={() => void logout()}>Ir a iniciar sesión</button>
    </section></main>
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (role && user?.role !== role) return <ForbiddenPage />
  return children
}
