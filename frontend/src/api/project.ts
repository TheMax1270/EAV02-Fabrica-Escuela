import { api } from './client'

export type ProjectStatus = 'IN_PROGRESS' | 'COMPLETED' | 'LOOKING_FOR_COLLABORATORS'

export type Project = {
  id: string
  userId: string
  username: string
  title: string
  description: string
  technologies: string[]
  status: ProjectStatus
  repositoryUrl: string | null
  createdAt: string
  updatedAt: string
}

export type ProjectPayload = Omit<Project, 'id' | 'userId' | 'username' | 'createdAt' | 'updatedAt'>

export async function createProject(payload: ProjectPayload): Promise<Project> {
  const response = await api.post<Project>('/api/v1/projects', payload)
  return response.data
}

export async function getMyProjects(): Promise<Project[]> {
  const response = await api.get<Project[]>('/api/v1/projects/me')
  return response.data
}

export async function updateProject(projectId: string, payload: ProjectPayload): Promise<Project> {
  const response = await api.put<Project>(`/api/v1/projects/${projectId}`, payload)
  return response.data
}