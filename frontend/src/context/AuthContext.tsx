import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import axios, { type InternalAxiosRequestConfig } from 'axios'
import { useNavigate } from 'react-router-dom'
import { api, setApiAccessToken } from '../api/client'
import { loginUser, logoutUser, refreshSession, type AuthUser, type LoginPayload } from '../api/auth'
import { AuthContext, type Session } from './auth-state'

type RetryableRequest = InternalAxiosRequestConfig & { _retriedAfterRefresh?: boolean }
const publicAuthPaths = [
  '/api/v1/auth/login', '/api/v1/auth/register',
  '/api/v1/auth/refresh', '/api/v1/auth/logout',
]

function isPublicAuthRequest(url?: string) {
  return publicAuthPaths.some((path) => url?.split('?')[0] === path)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [session, setSession] = useState<Session | null>(null)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const sessionRef = useRef<Session | null>(null)

  const replaceSession = useCallback((next: Session | null) => {
    sessionRef.current = next
    setApiAccessToken(next?.accessToken ?? null)
    setSession(next)
  }, [])

  const login = useCallback(async (payload: LoginPayload) => {
    const response = await loginUser(payload)
    replaceSession(response)
  }, [replaceSession])

  const logout = useCallback(async () => {
    if (isLoggingOut) return
    setIsLoggingOut(true)
    const refreshToken = sessionRef.current?.refreshToken
    try {
      if (refreshToken) await logoutUser(refreshToken)
    } catch {
      // Local credentials must be removed even when revocation cannot be reached.
    } finally {
      replaceSession(null)
      setIsLoggingOut(false)
      navigate('/login', { replace: true })
    }
  }, [isLoggingOut, navigate, replaceSession])

  const updateUser = useCallback((user: AuthUser) => {
    const current = sessionRef.current
    if (current) replaceSession({ ...current, user })
  }, [replaceSession])

  useEffect(() => {
    let refreshPromise: Promise<string> | null = null
    const responseId = api.interceptors.response.use(undefined, async (error: unknown) => {
      if (!axios.isAxiosError(error) || error.response?.status !== 401 || !error.config) {
        return Promise.reject(error)
      }
      const original = error.config as RetryableRequest
      if (original._retriedAfterRefresh || !original.url?.startsWith('/api/')
          || isPublicAuthRequest(original.url)) {
        return Promise.reject(error)
      }
      const current = sessionRef.current
      if (!current) return Promise.reject(error)
      original._retriedAfterRefresh = true

      const sentToken = original.headers.get('Authorization')
      if (sentToken !== `Bearer ${current.accessToken}`) {
        original.headers.set('Authorization', `Bearer ${current.accessToken}`)
        return api(original)
      }
      let accessToken: string
      try {
        if (!refreshPromise) {
          const refreshToken = current.refreshToken
          refreshPromise = refreshSession(refreshToken).then((next) => {
            if (sessionRef.current?.refreshToken !== refreshToken) {
              throw new Error('Session changed during refresh')
            }
            replaceSession(next)
            return next.accessToken
          }).finally(() => { refreshPromise = null })
        }
        accessToken = await refreshPromise
      } catch (failure) {
        if (sessionRef.current?.refreshToken === current.refreshToken) {
          replaceSession(null)
          navigate('/login', { replace: true })
        }
        return Promise.reject(failure)
      }
      original.headers.set('Authorization', `Bearer ${accessToken}`)
      return api(original)
    })

    return () => {
      api.interceptors.response.eject(responseId)
    }
  }, [navigate, replaceSession])

  return (
    <AuthContext.Provider value={{ user: session?.user ?? null, isAuthenticated: Boolean(session),
      isLoggingOut, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  )
}
