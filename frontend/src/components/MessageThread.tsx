import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getBlockSummary } from '../api/block'
import { interactionError } from '../api/interaction'
import { formatMessageDate, getMessages, type Participant, type PrivateMessage } from '../api/message'
import { useAuth } from '../context/useAuth'
import MessageForm from './MessageForm'

const POLL_INTERVAL_MS = 10_000

// Une mensajes sin duplicados y en orden cronológico: el polling y el envío pueden traer el mismo mensaje.
function mergeMessages(current: PrivateMessage[], incoming: PrivateMessage[]) {
  const byId = new Map(current.map((message) => [message.id, message]))
  incoming.forEach((message) => byId.set(message.id, message))
  return [...byId.values()].sort((a, b) => a.createdAt.localeCompare(b.createdAt) || a.id.localeCompare(b.id))
}

export default function MessageThread({ participantId, onActivity }: Readonly<{
  participantId: string; onActivity: () => void
}>) {
  const { user } = useAuth()
  const [participant, setParticipant] = useState<Participant | null>(null)
  const [messages, setMessages] = useState<PrivateMessage[]>([])
  const [hasOlder, setHasOlder] = useState(false)
  const [blocked, setBlocked] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadingOlder, setLoadingOlder] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const listRef = useRef<HTMLOListElement>(null)
  const lastId = messages.at(-1)?.id ?? null
  const lastIdRef = useRef<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([getMessages(participantId, {}, controller.signal), getBlockSummary(participantId, controller.signal)])
      .then(([slice, block]) => {
        if (controller.signal.aborted) return
        setParticipant(slice.participant); setMessages(slice.content); setHasOlder(slice.hasMore)
        setBlocked(block.blockedByMe || block.blockedMe); setError(null)
      }).catch((failure: unknown) => {
        if (!controller.signal.aborted) setError(interactionError(failure, 'No pudimos cargar la conversación.'))
      }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [participantId])

  useEffect(() => {
    if (loading || error) return undefined
    const controller = new AbortController()
    const timer = window.setInterval(() => {
      if (document.visibilityState !== 'visible') return
      const after = lastIdRef.current
      getMessages(participantId, after ? { after } : {}, controller.signal).then((slice) => {
        if (controller.signal.aborted || slice.content.length === 0) return
        setMessages((current) => mergeMessages(current, slice.content)); onActivity()
      }).catch(() => { /* Silencioso: el siguiente ciclo vuelve a intentarlo. */ })
    }, POLL_INTERVAL_MS)
    return () => { window.clearInterval(timer); controller.abort() }
  }, [participantId, loading, error, onActivity])

  // Solo baja al final cuando cambia el último mensaje, no al cargar mensajes anteriores.
  useEffect(() => {
    lastIdRef.current = lastId
    const list = listRef.current
    if (list) list.scrollTop = list.scrollHeight
  }, [lastId])

  async function loadOlder() {
    const oldest = messages[0]
    if (!oldest) return
    setLoadingOlder(true)
    try {
      const slice = await getMessages(participantId, { before: oldest.id })
      setMessages((current) => mergeMessages(current, slice.content)); setHasOlder(slice.hasMore)
    } catch (failure: unknown) {
      setError(interactionError(failure, 'No pudimos cargar mensajes anteriores.'))
    } finally { setLoadingOlder(false) }
  }

  function onSent(message: PrivateMessage) {
    setMessages((current) => mergeMessages(current, [message])); onActivity()
  }

  if (loading) return <p className="loading-note" role="status">Cargando conversación...</p>
  if (error && !participant) return <p className="form-error" role="alert">{error}</p>

  return <section className="message-thread" aria-labelledby="thread-title">
    <header className="message-thread-header">
      <Link className="thread-back" to="/messages">← Conversaciones</Link>
      {participant && <Link className="thread-participant" to={`/developers/${participant.id}`}>
        <span className="profile-avatar" aria-hidden="true">{participant.username.charAt(0).toUpperCase()}</span>
        <h2 id="thread-title">@{participant.username}</h2>
      </Link>}
    </header>
    <ol className="message-list" ref={listRef} aria-live="polite">
      {hasOlder && <li className="message-older">
        <button className="text-button" type="button" disabled={loadingOlder} onClick={() => void loadOlder()}>
          {loadingOlder ? 'Cargando...' : 'Cargar anteriores'}
        </button>
      </li>}
      {messages.length === 0 && <li className="message-empty">Aún no hay mensajes. Escribe el primero.</li>}
      {messages.map((message) => {
        const mine = message.senderId === user?.id
        return <li key={message.id} className={`message-bubble ${mine ? 'mine' : 'theirs'}`}>
          <p>{message.content}</p>
          <time dateTime={message.createdAt}>{formatMessageDate(message.createdAt)}</time>
        </li>
      })}
    </ol>
    {error && <p className="field-error" role="alert">{error}</p>}
    {blocked
      ? <p className="blocked-note message-blocked">No puedes enviar mensajes en esta conversación porque existe un bloqueo.</p>
      : <MessageForm participantId={participantId} onSent={onSent} />}
  </section>
}
