import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getFollowSummary, setFollowing, type FollowSummary } from '../api/follow'
import { interactionError } from '../api/interaction'
import { useAuth } from '../context/useAuth'

export default function FollowButton({ userId, onChange }: Readonly<{ userId: string; onChange?: () => void }>) {
  const { user, isAuthenticated } = useAuth()
  const [summary, setSummary] = useState<FollowSummary | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [reload, setReload] = useState(0)
  const sending = useRef(false)
  const ownProfile = user?.id === userId
  useEffect(() => {
    const controller = new AbortController()
    getFollowSummary(userId, controller.signal).then((result) => {
      if (!controller.signal.aborted) { setSummary(result); setError(null) }
    }).catch((failure: unknown) => {
      if (!controller.signal.aborted) setError(interactionError(failure, 'No pudimos cargar los seguidores.'))
    })
    return () => controller.abort()
  }, [userId, user?.id, reload])
  async function toggle() {
    if (!summary || sending.current) return
    sending.current = true; setBusy(true); setError(null)
    try {
      await setFollowing(userId, !summary.followedByMe)
      setSummary(await getFollowSummary(userId))
      onChange?.()
    } catch (failure: unknown) {
      setError(interactionError(failure, 'No pudimos actualizar el seguimiento.'))
    } finally { sending.current = false; setBusy(false) }
  }
  const followLabel = summary?.followedByMe ? 'Siguiendo' : 'Seguir'
  return <div className="follow-controls">
    {!summary && !error && <span role="status">Cargando seguidores...</span>}
    {summary && <div className="follow-row">
      <span><strong>{summary.followersCount}</strong> {summary.followersCount === 1 ? 'seguidor' : 'seguidores'}</span>
      <span><strong>{summary.followingCount}</strong> seguidos</span>
      {isAuthenticated && !ownProfile && <button type="button" disabled={busy} aria-pressed={Boolean(summary.followedByMe)}
        className={`button ${summary.followedByMe ? 'button-secondary' : 'button-primary'}`} onClick={() => void toggle()}>
        {followLabel}
      </button>}
      {!isAuthenticated && <Link className="interaction-login" to="/login">Inicia sesión para seguir</Link>}
    </div>}
    {error && <div role="alert"><p className="field-error">{error}</p><button className="text-button" type="button"
      disabled={busy} onClick={() => { setError(null); setReload((value) => value + 1) }}>Reintentar</button></div>}
  </div>
}
