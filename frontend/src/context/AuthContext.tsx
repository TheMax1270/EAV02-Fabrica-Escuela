import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import axios, { type InternalAxiosRequestConfig } from 'axios'
import { useNavigate } from 'react-router-dom'
import { api, setApiAccessToken } from '../api/client'
import { loginUser, logoutUser, type AuthUser, type LoginPayload } from '../api/auth'
import { readRefreshToken, storeRefreshToken, refreshOnce, invalidRefresh } from './session-storage'
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
  const [isRestoringSession, setIsRestoringSession] = useState(true)
  const [sessionRestoreError, setSessionRestoreError] = useState<string | null>(null)
  const generation = useRef(0)
  const restoration = useRef<Promise<void> | null>(null)
  const loggingOut = useRef(false)

  const replaceSession = useCallback((next: Session | null) => {
    storeRefreshToken(next?.refreshToken ?? null)
    sessionRef.current = next
    setApiAccessToken(next?.accessToken ?? null)
    setSession(next)
  }, [])

  const restoreSession = useCallback(() => {
    if (restoration.current) return restoration.current
    const version = generation.current
    const task = (async () => {
      await Promise.resolve()
      if (generation.current !== version) return
      setIsRestoringSession(true)
      setSessionRestoreError(null)
      const token = readRefreshToken()
      if (!token) { setIsRestoringSession(false); return }
      try {
        const next = await refreshOnce(token)
        if (generation.current === version) replaceSession(next)
      } catch (failure: unknown) {
        if (generation.current !== version) return
        if (invalidRefresh(failure)) replaceSession(null)
        else setSessionRestoreError('No pudimos restaurar tu sesión. Comprueba la conexión e intenta de nuevo.')
      } finally {
        if (generation.current === version) setIsRestoringSession(false)
      }
    })()
    restoration.current = task
    void task.finally(() => { if (restoration.current === task) restoration.current = null })
    return task
  }, [replaceSession])

  useEffect(() => { void restoreSession() }, [restoreSession])

  const login = useCallback(async (payload: LoginPayload) => {
    generation.current += 1
    const version = generation.current
    try {
      const response = await loginUser(payload)
      if (generation.current !== version) return
      replaceSession(response)
      setSessionRestoreError(null)
    } finally {
      if (generation.current === version) setIsRestoringSession(false)
    }
  }, [replaceSession])

  const logout = useCallback(async () => {
    if (loggingOut.current) return
    loggingOut.current = true
    const version = ++generation.current
    setIsLoggingOut(true)
    const refreshToken = sessionRef.current?.refreshToken ?? readRefreshToken()
    replaceSession(null)
    setSessionRestoreError(null)
    setIsRestoringSession(false)
    try {
      if (refreshToken) await logoutUser(refreshToken)
    } catch {
      // Local credentials must be removed even when revocation cannot be reached.
    } finally {
      loggingOut.current = false
      setIsLoggingOut(false)
      if (generation.current === version) {
        replaceSession(null)
        navigate('/login', { replace: true })
      }
    }
  }, [navigate, replaceSession])

  const updateUser = useCallback((user: AuthUser) => {
    const current = sessionRef.current
    if (current) replaceSession({ ...current, user })
  }, [replaceSession])

  useEffect(() => {
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
      const version = generation.current
      try {
        const next = await refreshOnce(current.refreshToken)
        if (generation.current !== version) return Promise.reject(error)
        if (sessionRef.current?.refreshToken === current.refreshToken) replaceSession(next)
        else if (sessionRef.current?.refreshToken !== next.refreshToken) return Promise.reject(error)
        accessToken = next.accessToken
      } catch (failure) {
        if (generation.current === version && sessionRef.current?.refreshToken === current.refreshToken
            && invalidRefresh(failure)) {
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
      isLoggingOut, isRestoringSession, sessionRestoreError, retrySessionRestore: restoreSession, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  )
}
