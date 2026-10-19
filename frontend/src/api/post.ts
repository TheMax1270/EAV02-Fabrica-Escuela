import { api } from './client'

export type Post = {
  id: string
  userId: string
  username: string
  title: string | null
  content: string
  createdAt: string
  type: 'GENERAL' | 'TECHNICAL_DISCUSSION'
  link: string | null
  categories: string[]
  resolved: boolean
}

export type PostPayload = {
  type: Post['type']
  title?: string
  content: string
  link?: string
  categories?: string[]
}

export type PostPage = {
  content: Post[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listPosts(page: number, size: number, signal?: AbortSignal): Promise<PostPage> {
  const response = await api.get<PostPage>('/api/v1/posts', { params: { page, size }, signal })
  return response.data
}

export async function createPost(payload: PostPayload): Promise<Post> {
  const response = await api.post<Post>('/api/v1/posts', payload)
  return response.data
}

export async function resolvePost(postId: string): Promise<Post> {
  const response = await api.patch<Post>(`/api/v1/posts/${postId}/resolve`)
  return response.data
}

export async function deletePost(postId: string): Promise<void> {
  await api.delete(`/api/v1/posts/${postId}`)
}
