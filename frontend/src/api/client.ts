import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
})

const publicAuthPaths = new Set([
  '/api/v1/auth/login',
  '/api/v1/auth/register',
  '/api/v1/auth/refresh',
  '/api/v1/auth/logout',
])

let accessToken: string | null = null

export function setApiAccessToken(token: string | null) {
  accessToken = token
}

api.interceptors.request.use((config) => {
  const path = config.url?.split('?')[0]
  if (accessToken && path?.startsWith('/api/') && !publicAuthPaths.has(path)) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})
