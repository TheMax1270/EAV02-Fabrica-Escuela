import { api } from './client'

export type BlockSummary = { blockedByMe: boolean; blockedMe: boolean }

function userPath(userId: string) {
  return `/api/v1/users/${userId}`
}

export async function getBlockSummary(userId: string, signal?: AbortSignal) {
  return (await api.get<BlockSummary>(`${userPath(userId)}/block-summary`, { signal })).data
}

export async function setBlocked(userId: string, blocked: boolean) {
  const path = `${userPath(userId)}/blocks/me`
  if (blocked) await api.put(path)
  else await api.delete(path)
}
