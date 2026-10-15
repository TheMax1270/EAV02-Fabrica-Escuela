import axios from 'axios'
import { refreshSession, type LoginResponse } from '../api/auth'

const REFRESH_TOKEN_KEY = 'devconnect.refreshToken'
const pending = new Map<string, Promise<LoginResponse>>()

export function readRefreshToken(): string | null {
  try { return sessionStorage.getItem(REFRESH_TOKEN_KEY) }
  catch { return null }
}

export function storeRefreshToken(token: string | null) {
  try {
    if (token) sessionStorage.setItem(REFRESH_TOKEN_KEY, token)
    else sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  } catch {
    // Restricted browser storage must not prevent in-memory login or logout.
  }
}

export function invalidRefresh(error: unknown) {
  return axios.isAxiosError(error) && (error.response?.status === 401 || error.response?.status === 403)
}

export function refreshOnce(token: string): Promise<LoginResponse> {
  const existing = pending.get(token)
  if (existing) return existing
  const request = refreshSession(token).then((next) => {
    if (readRefreshToken() === token) storeRefreshToken(next.refreshToken)
    return next
  }).finally(() => { pending.delete(token) })
  pending.set(token, request)
  return request
}
