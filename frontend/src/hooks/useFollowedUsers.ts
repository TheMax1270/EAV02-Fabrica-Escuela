import { useEffect, useMemo, useState } from 'react'
import { getFollowedAmong } from '../api/follow'
import { useAuth } from '../context/useAuth'

const MAX_IDS = 100
const EMPTY = new Set<string>()

export function useFollowedUsers(userIds: string[]): Set<string> {
  const { user, isAuthenticated } = useAuth()
  const key = useMemo(() => {
    const unique = new Set(userIds.filter((id) => id !== user?.id))
    return [...unique].sort((a, b) => a.localeCompare(b)).slice(0, MAX_IDS).join(',')
  }, [userIds, user?.id])
  const [result, setResult] = useState<{ key: string; followed: Set<string> }>({ key: '', followed: EMPTY })
  useEffect(() => {
    if (!isAuthenticated || !key) return undefined
    const controller = new AbortController()
    getFollowedAmong(key.split(','), controller.signal).then((ids) => {
      if (!controller.signal.aborted) setResult({ key, followed: new Set(ids) })
    }).catch(() => {
      if (!controller.signal.aborted) setResult({ key, followed: EMPTY })
    })
    return () => controller.abort()
  }, [key, isAuthenticated])
  return isAuthenticated && result.key === key ? result.followed : EMPTY
}
