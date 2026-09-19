import { api } from './client'

export type ExperienceLevel = 'JUNIOR' | 'SEMI_SENIOR' | 'SENIOR'

export type Profile = {
  id: string
  userId: string
  username: string
  biography: string
  programmingLanguages: string[]
  technologies: string[]
  experienceLevel: ExperienceLevel
  githubUrl: string | null
  linkedinUrl: string | null
  portfolioUrl: string | null
  createdAt: string
  updatedAt: string
}

export type ProfilePayload = Omit<Profile, 'id' | 'userId' | 'username' | 'createdAt' | 'updatedAt'>

export async function getOwnProfile(): Promise<Profile> {
  const response = await api.get<Profile>('/api/v1/profile/me')
  return response.data
}

export async function createProfile(payload: ProfilePayload): Promise<Profile> {
  const response = await api.post<Profile>('/api/v1/profile/me', payload)
  return response.data
}

export async function updateOwnProfile(payload: ProfilePayload): Promise<Profile> {
  const response = await api.put<Profile>('/api/v1/profile/me', payload)
  return response.data
}
