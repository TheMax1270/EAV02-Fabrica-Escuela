import { api } from './client'

export type AuthUser = {
  id: string
  username: string
  role: 'DEVELOPER' | 'ADMIN'
  enabled: boolean
}

export type LoginPayload = { identifier: string; password: string }

export type LoginResponse = {
  accessToken: string
  refreshToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: AuthUser
}

export async function loginUser(payload: LoginPayload): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>('/api/v1/auth/login', payload)
  return response.data
}

export async function refreshSession(refreshToken: string): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>('/api/v1/auth/refresh', { refreshToken })
  return response.data
}

export async function logoutUser(refreshToken: string): Promise<void> {
  await api.post('/api/v1/auth/logout', { refreshToken })
}

export async function getCurrentUser(): Promise<AuthUser> {
  const response = await api.get<AuthUser>('/api/v1/auth/me')
  return response.data
}

export type RegistrationPayload = {
  fullName: string
  username: string
  email: string
  password: string
  passwordConfirmation: string
}

export type RegistrationResponse = {
  id: string
  fullName: string
  username: string
  email: string
  role: 'DEVELOPER' | 'ADMIN'
  enabled: boolean
  createdAt: string
}

export type ApiErrorResponse = {
  code?: string
  message?: string
  errors?: Record<string, string>
}

export async function registerUser(payload: RegistrationPayload): Promise<RegistrationResponse> {
  const response = await api.post<RegistrationResponse>('/api/v1/auth/register', payload)
  return response.data
}
