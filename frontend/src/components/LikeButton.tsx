import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getLikes, setLike, interactionError, type LikeSummary, type Target } from '../api/interaction'
import { useAuth } from '../context/useAuth'
export default function LikeButton({ target }: { target: Target }) {
  const { user, isAuthenticated } = useAuth()
  const [summary, setSummary] = useState<LikeSummary | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [showPeople, setShowPeople] = useState(false)
  const [reload, setReload] = useState(0)
  const sending = useRef(false)
  const { kind, id } = target
  useEffect(() => {
    const controller = new AbortController()
    getLikes({ kind, id }, controller.signal).then((result) => {
      if (!controller.signal.aborted) { setSummary(result); setError(null) }
    }).catch((failure: unknown) => {
      if (!controller.signal.aborted) setError(interactionError(failure, 'No pudimos cargar los Me gusta.'))
    })
    return () => controller.abort()
  }, [kind, id, user?.id, reload])
  async function toggle() {
    if (!summary || !isAuthenticated || sending.current) return
    sending.current = true; setBusy(true); setError(null)
    try { await setLike(target, !summary.likedByMe); setSummary(await getLikes(target)) }
    catch (failure: unknown) {
      setSummary(null)
      setError(interactionError(failure, 'No pudimos actualizar los Me gusta. Recarga el estado antes de intentarlo de nuevo.'))
    } finally { sending.current = false; setBusy(false) }
  }
  return <div className="like-controls">
    {!summary && !error && <span role="status">Cargando Me gusta...</span>}
    {summary && <div className="like-row">{isAuthenticated ? <button className={`like-button ${summary.likedByMe ? 'like-active' : ''}`}
      type="button" aria-pressed={Boolean(summary.likedByMe)} disabled={busy} onClick={() => void toggle()}>
      <span aria-hidden="true">♥</span> {summary.likedByMe ? 'Te gusta' : 'Me gusta'}
    </button> : <span className="like-button login-like"><span aria-hidden="true">♥</span> Me gusta</span>}
      <button className="like-count" type="button" disabled={summary.likeCount === 0} onClick={() => setShowPeople((value) => !value)}>
        {summary.likeCount} {summary.likeCount === 1 ? 'Me gusta' : 'Me gusta'}
      </button></div>}
    {!isAuthenticated && summary && <Link className="interaction-login" to="/login">Inicia sesión para reaccionar</Link>}
    {showPeople && summary && <div className="like-people" aria-label="Personas que dieron Me gusta">
      {summary.usernames.length ? summary.usernames.map((username) => <span key={username}>@{username}</span>) : <span>Aún no hay reacciones.</span>}
    </div>}
    {error && <div role="alert"><p className="field-error">{error}</p><button className="text-button" type="button"
      disabled={busy} onClick={() => { setError(null); setReload((value) => value + 1) }}>Reintentar</button></div>}
  </div>
}
