import { useEffect, useRef, useState } from 'react'
import { Ban } from 'lucide-react'

import { getBlockSummary, setBlocked, type BlockSummary } from '../api/block'
import { interactionError } from '../api/interaction'
import { useAuth } from '../context/useAuth'

export default function BlockButton({ userId, onChange, reloadKey = 0 }: Readonly<{
  userId: string; compact?: boolean
  onChange?: () => void
  reloadKey?: number
}>) {
  const { user, isAuthenticated } = useAuth()
  const [summary, setSummary] = useState<BlockSummary | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [feedback, setFeedback] = useState<string | null>(null)
  const sending = useRef(false)
  const ownProfile = user?.id === userId

  useEffect(() => {
    const controller = new AbortController()
    let timedOut = false
    const timeout = window.setTimeout(() => { timedOut = true; controller.abort() }, 8000)
    getBlockSummary(userId, controller.signal).then((result) => {
      if (!controller.signal.aborted) { setSummary(result); setError(null) }
    }).catch((failure: unknown) => {
      if (!controller.signal.aborted || timedOut) setError(interactionError(failure, 'No pudimos cargar el estado de bloqueo.'))
    })
    return () => { window.clearTimeout(timeout); controller.abort() }
  }, [userId, user?.id, reloadKey])

  async function toggle() {
    if (!summary || sending.current) return
    sending.current = true
    setBusy(true)
    setError(null)
    try {
      await setBlocked(userId, !summary.blockedByMe)
      setSummary(await getBlockSummary(userId))
      setFeedback(summary.blockedByMe
        ? 'Usuario desbloqueado. Las interacciones permitidas vuelven a estar disponibles.'
        : 'Usuario bloqueado. Ya no podrá seguirte ni iniciar nuevas interacciones contigo.')
      onChange?.()
    } catch (failure: unknown) {
      setError(interactionError(failure, 'No pudimos actualizar el bloqueo.'))
    } finally {
      sending.current = false
      setBusy(false)
    }
  }

  if (!isAuthenticated || ownProfile) return null
  if (!summary && !error) return <span className="loading-note" role="status">Cargando bloqueo...</span>
  if (error) return <div role="alert"><p className="field-error">{error}</p></div>

  return <div className="block-controls">
    <button className={`button ${summary?.blockedByMe ? 'button-secondary' : 'button-danger'}`} type="button"
      disabled={busy} onClick={() => { setFeedback(null); setConfirmOpen(true) }}>
      <Ban size={20} strokeWidth={1.8} aria-hidden="true" /> {busy ? 'Actualizando...' : summary?.blockedByMe ? 'Desbloquear' : 'Bloquear usuario'}
    </button>
    {summary?.blockedMe && <p className="field-error" role="status">Este usuario te ha bloqueado.</p>}
    {feedback && <p className="block-feedback" role="status">{feedback}</p>}
    {confirmOpen && <div className="modal-overlay" role="presentation">
      <div className="confirm-modal" role="dialog" aria-modal="true" aria-labelledby="block-confirm-title">
        <p className="eyebrow">{summary?.blockedByMe ? 'DESBLOQUEAR USUARIO' : 'BLOQUEAR USUARIO'}</p>
        <h2 id="block-confirm-title">{summary?.blockedByMe ? '¿Desbloquear este usuario?' : '¿Bloquear este usuario?'}</h2>
        <p>{summary?.blockedByMe
          ? 'El usuario podrá volver a seguirte e interactuar contigo.'
          : 'El usuario no podrá seguirte ni iniciar nuevas interacciones privadas contigo.'}</p>
        <div className="confirm-modal-actions">
          <button className="button button-secondary" type="button" disabled={busy} onClick={() => setConfirmOpen(false)}>Cancelar</button>
          <button className={`button ${summary?.blockedByMe ? 'button-primary' : 'button-danger'}`} type="button"
            disabled={busy} onClick={() => { setConfirmOpen(false); void toggle() }}>
            {busy ? 'Actualizando...' : summary?.blockedByMe ? 'Desbloquear' : 'Bloquear'}
          </button>
        </div>
      </div>
    </div>}
  </div>
}
