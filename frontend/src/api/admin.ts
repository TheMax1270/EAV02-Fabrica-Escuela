import { api } from './client'
import type { AuthUser } from './auth'

export type AccountStatus = 'ACTIVE' | 'SUSPENDED'

export type AdminUser = {
  id: string
  fullName: string
  username: string
  email: string
  role: AuthUser['role']
  status: AccountStatus
  createdAt: string
}

export type UserPage = {
  content: AdminUser[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ListUsersParams = { page: number; size: number; status?: AccountStatus }

export async function listUsers(params: ListUsersParams): Promise<UserPage> {
  const response = await api.get<UserPage>('/api/v1/admin/users', { params })
  return response.data
}

export async function suspendUser(userId: string): Promise<AdminUser> {
  const response = await api.post<AdminUser>(`/api/v1/admin/users/${userId}/suspend`)
  return response.data
}

export async function reactivateUser(userId: string): Promise<AdminUser> {
  const response = await api.post<AdminUser>(`/api/v1/admin/users/${userId}/reactivate`)
  return response.data
}
