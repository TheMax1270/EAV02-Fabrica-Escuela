import { useRef, useState, type FormEvent } from 'react'
import { sendComment, interactionError } from '../api/interaction'
import { useAuth } from '../context/useAuth'
import { Link } from 'react-router-dom'

export default function CommentForm({ path, reply = false, onCreated, onCancel }: {
  path: string; reply?: boolean; onCreated: () => void; onCancel?: () => void
}) {
  const { isAuthenticated } = useAuth()
  const [content, setContent] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const sending = useRef(false)
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!isAuthenticated || sending.current) return
    if (!content.trim() || content.trim().length > 2000) {
      setError('Escribe un comentario de entre 1 y 2.000 caracteres.'); return
    }
    sending.current = true
    setBusy(true); setError(null)
    try { await sendComment(path, content.trim()); setContent(''); onCreated() }
    catch (failure: unknown) { setError(interactionError(failure, 'No pudimos enviar tu comentario. Tu texto se conserva.')) }
    finally { sending.current = false; setBusy(false) }
  }
  if (!isAuthenticated) return <p className="interaction-login"><Link to="/login">Inicia sesión</Link> para {reply ? 'responder' : 'comentar'}.</p>
  return <form className="comment-form" onSubmit={submit} noValidate aria-busy={busy}>
    <label>{reply ? 'Tu respuesta' : 'Tu comentario'}
      <textarea value={content} rows={3} maxLength={2000} disabled={busy} required
        aria-invalid={Boolean(error)} onChange={(event) => { setContent(event.target.value); setError(null) }} />
    </label>
    <div className="comment-form-actions"><span>{content.length} / 2.000</span>
      <div className="comment-form-buttons">
        {onCancel && <button className="comment-cancel" type="button" disabled={busy} onClick={onCancel}>Cancelar</button>}
        <button className="button button-secondary" disabled={busy} type="submit">{busy ? 'Enviando...' : reply ? 'Responder' : 'Comentar'}</button>
      </div>
    </div>
    {error && <p className="field-error" role="alert">{error}</p>}
  </form>
}
