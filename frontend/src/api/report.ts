import { api } from './client'
import axios from 'axios'
import type { ApiErrorResponse } from './auth'

export type ReportReason = 'SPAM' | 'HARASSMENT' | 'INAPPROPRIATE_CONTENT' | 'MISINFORMATION' | 'OTHER'

export type ReportPayload = { reason: ReportReason; details?: string }

export function reportError(error: unknown, fallback: string) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    if (error.response?.status === 409) return 'Ya reportaste este elemento y sigue pendiente de revisión.'
    if (error.response?.status === 401) return 'Inicia sesión para reportar.'
  }
  return fallback
}

export async function reportPost(postId: string, payload: ReportPayload) {
  await api.post(`/api/v1/posts/${postId}/reports`, payload)
}

export async function reportComment(commentId: string, payload: ReportPayload) {
  await api.post(`/api/v1/comments/${commentId}/reports`, payload)
}

export async function reportProfile(profileId: string, payload: ReportPayload) {
  await api.post(`/api/v1/profiles/${profileId}/reports`, payload)
}