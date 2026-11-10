import { useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { Send } from 'lucide-react'
import { interactionError } from '../api/interaction'
import { MESSAGE_MAX_LENGTH, sendMessage, type PrivateMessage } from '../api/message'

export default function MessageForm({ participantId, onSent }: Readonly<{
  participantId: string; onSent: (message: PrivateMessage) => void
}>) {
  const [content, setContent] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const sending = useRef(false)
  const trimmed = content.trim()

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (sending.current) return
    if (!trimmed || trimmed.length > MESSAGE_MAX_LENGTH) {
      setError('Escribe un mensaje de entre 1 y 2.000 caracteres.'); return
    }
    sending.current = true
    setBusy(true); setError(null)
    try { onSent(await sendMessage(participantId, trimmed)); setContent('') }
    catch (failure: unknown) { setError(interactionError(failure, 'No pudimos enviar tu mensaje. Tu texto se conserva.')) }
    finally { sending.current = false; setBusy(false) }
  }

  // Enter envía; Shift+Enter inserta un salto de línea.
  function onKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault()
      event.currentTarget.form?.requestSubmit()
    }
  }

  return <form className="message-form" onSubmit={submit} noValidate aria-busy={busy}>
    <label className="visually-hidden" htmlFor="message-content">Escribe un mensaje</label>
    <textarea id="message-content" value={content} rows={2} maxLength={MESSAGE_MAX_LENGTH} disabled={busy}
      placeholder="Escribe un mensaje..." aria-invalid={Boolean(error)} onKeyDown={onKeyDown}
      onChange={(event) => { setContent(event.target.value); setError(null) }} />
    <div className="message-form-actions">
      <span>{content.length} / 2.000</span>
      <button className="button button-primary" type="submit" disabled={busy || !trimmed}>
        <Send size={16} strokeWidth={1.8} aria-hidden="true" /> {busy ? 'Enviando...' : 'Enviar'}
      </button>
    </div>
    {error && <p className="field-error" role="alert">{error}</p>}
  </form>
}
