import { api } from './client'

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
