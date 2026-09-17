import { createContext } from 'react'
import type { AuthUser, LoginPayload, LoginResponse } from '../api/auth'

export type Session = Pick<LoginResponse, 'accessToken' | 'refreshToken' | 'user'>

export type AuthContextValue = {
  user: AuthUser | null
  isAuthenticated: boolean
  isLoggingOut: boolean
  login: (payload: LoginPayload) => Promise<void>
  logout: () => Promise<void>
  updateUser: (user: AuthUser) => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
