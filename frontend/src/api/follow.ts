import { api } from './client'

export type FollowSummary = { followersCount: number; followingCount: number; followedByMe: boolean | null }
export type FollowUser = { userId: string; username: string; followedAt: string }
export type FollowPage = { content: FollowUser[]; page: number; size: number; totalElements: number; totalPages: number }
export type FollowListKind = 'followers' | 'following'

function userPath(userId: string) { return `/api/v1/users/${userId}` }

export async function getFollowSummary(userId: string, signal?: AbortSignal) {
  return (await api.get<FollowSummary>(`${userPath(userId)}/follow-summary`, { signal })).data
}
export async function setFollowing(userId: string, follow: boolean) {
  const path = `${userPath(userId)}/followers/me`
  if (follow) await api.put(path)
  else await api.delete(path)
}
export async function listFollows(userId: string, kind: FollowListKind, page: number, signal?: AbortSignal) {
  return (await api.get<FollowPage>(`${userPath(userId)}/${kind}`, { params: { page, size: 20 }, signal })).data
}
