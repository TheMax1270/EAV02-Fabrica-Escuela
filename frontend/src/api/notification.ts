import { api } from './client'

export type NotificationType = 'COMMENT' | 'LIKE' | 'FOLLOWER' | 'MESSAGE'
export type NotificationTargetType = 'POST' | 'PROJECT' | 'USER'

// Se llama AppNotification para no chocar con el tipo global Notification del navegador.
export type AppNotification = {
  id: string
  type: NotificationType
  actorId: string
  actorUsername: string
  targetType: NotificationTargetType
  targetId: string
  targetLabel: string | null
  createdAt: string
  read: boolean
}

export type NotificationPage = {
  content: AppNotification[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const NOTIFICATIONS_CHANGED_EVENT = 'devconnect:notifications-changed'

export function notifyNotificationsChanged() {
  window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED_EVENT))
}

export async function listNotifications(page: number, signal?: AbortSignal) {
  return (await api.get<NotificationPage>('/api/v1/notifications', { params: { page, size: 20 }, signal })).data
}

export async function getUnreadCount(signal?: AbortSignal) {
  return (await api.get<{ count: number }>('/api/v1/notifications/unread-count', { signal })).data.count
}

export async function markNotificationRead(notificationId: string) {
  return (await api.patch<AppNotification>(`/api/v1/notifications/${notificationId}/read`)).data
}

export async function markAllNotificationsRead() {
  await api.patch('/api/v1/notifications/read-all')
}