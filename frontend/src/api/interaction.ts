import { api } from './client'
import axios from 'axios'
import type { ApiErrorResponse } from './auth'

export function interactionError(error: unknown, fallback: string) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    if (error.response?.status === 401) return 'Inicia sesión para continuar.'
    if (error.response?.data?.message && error.response.status < 500) return error.response.data.message
  }
  return fallback
}
export type Target = { kind: 'projects' | 'posts'; id: string }
export type Comment = {
  id: string; userId: string; username: string; projectId: string | null; postId: string | null
  parentCommentId: string | null; content: string; createdAt: string; replyCount: number
}
export type CommentPage = { content: Comment[]; page: number; size: number; totalElements: number; totalPages: number }
export type LikeSummary = { likeCount: number; likedByMe: boolean | null; usernames: string[] }
export function targetPath(target: Target) { return `/api/v1/${target.kind}/${target.id}` }
export async function listComments(path: string, page: number, signal?: AbortSignal) {
  return (await api.get<CommentPage>(path, { params: { page, size: 20 }, signal })).data
}
export async function getCommentCount(target: Target) {
  return (await api.get<{ count: number }>(`${targetPath(target)}/comments/count`)).data.count
}
export async function getReplyCount(commentId: string) {
  return (await api.get<CommentPage>(`/api/v1/comments/${commentId}/replies`, {
    params: { page: 0, size: 1 },
  })).data.totalElements
}
export async function sendComment(path: string, content: string) {
  return (await api.post<Comment>(path, { content })).data
}
export async function deleteComment(commentId: string) {
  await api.delete(`/api/v1/comments/${commentId}`)
}
export async function getLikes(target: Target, signal?: AbortSignal) {
  return (await api.get<LikeSummary>(`${targetPath(target)}/likes`, { signal })).data
}
export async function setLike(target: Target, liked: boolean) {
  const path = `${targetPath(target)}/likes/me`
  if (liked) await api.put(path)
  else await api.delete(path)
}
