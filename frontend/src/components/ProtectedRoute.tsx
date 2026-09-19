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
  const { user, isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (role && user?.role !== role) return <ForbiddenPage />
  return children
}
