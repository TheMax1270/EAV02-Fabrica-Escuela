import { api } from './client'

export type PrivateMessage = {
  id: string; conversationId: string; senderId: string; recipientId: string; content: string; createdAt: string
}
export type Participant = { id: string; username: string }
export type MessageSlice = { participant: Participant; content: PrivateMessage[]; hasMore: boolean }
export type MessageCursor = { before?: string; after?: string }
export type ConversationSummary = {
  conversationId: string; participant: Participant; lastMessage: PrivateMessage | null; lastMessageAt: string
}
export type ConversationPage = {
  content: ConversationSummary[]; page: number; size: number; totalElements: number; totalPages: number
}

export const MESSAGE_MAX_LENGTH = 2000

function messagesPath(participantId: string) { return `/api/v1/conversations/${participantId}/messages` }

export async function listConversations(page: number, signal?: AbortSignal) {
  return (await api.get<ConversationPage>('/api/v1/conversations', { params: { page, size: 20 }, signal })).data
}
export async function getMessages(participantId: string, cursor: MessageCursor = {}, signal?: AbortSignal) {
  return (await api.get<MessageSlice>(messagesPath(participantId), { params: { ...cursor, limit: 30 }, signal })).data
}
export async function sendMessage(participantId: string, content: string) {
  return (await api.post<PrivateMessage>(messagesPath(participantId), { content })).data
}

export function formatMessageDate(value: string) {
  return new Date(value).toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' })
}
