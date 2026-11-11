import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { interactionError } from '../api/interaction'
import { formatMessageDate, listConversations, type ConversationPage, type ConversationSummary } from '../api/message'
import { useAuth } from '../context/useAuth'

const POLL_INTERVAL_MS = 10_000

// La primera página fresca manda; las conversaciones ya cargadas de páginas siguientes se conservan.
function mergeConversations(current: ConversationSummary[], fresh: ConversationSummary[]) {
  const freshIds = new Set(fresh.map((item) => item.conversationId))
  return [...fresh, ...current.filter((item) => !freshIds.has(item.conversationId))]
    .sort((a, b) => b.lastMessageAt.localeCompare(a.lastMessageAt))
}

export default function ConversationList({ activeId, refreshKey }: Readonly<{
  activeId?: string; refreshKey: number
}>) {
  const { user } = useAuth()
  const [items, setItems] = useState<ConversationSummary[]>([])
  const [page, setPage] = useState<ConversationPage | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const controller = new AbortController()
    const refresh = () => listConversations(0, controller.signal).then((result) => {
      if (controller.signal.aborted) return
      setItems((current) => mergeConversations(current, result.content))
      setPage((current) => current ?? result); setError(null)
    })
    refresh().catch((failure: unknown) => {
      if (!controller.signal.aborted) setError(interactionError(failure, 'No pudimos cargar tus conversaciones.'))
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') refresh().catch(() => { /* Reintenta en el siguiente ciclo. */ })
    }, POLL_INTERVAL_MS)
    return () => { window.clearInterval(timer); controller.abort() }
  }, [refreshKey])

  async function loadMore() {
    if (!page) return
    setLoading(true)
    try {
      const result = await listConversations(page.page + 1)
      setItems((current) => mergeConversations(current, result.content)); setPage(result)
    } catch (failure: unknown) {
      setError(interactionError(failure, 'No pudimos cargar más conversaciones.'))
    } finally { setLoading(false) }
  }

  const hasMore = page !== null && page.page + 1 < page.totalPages
  return <>
    {error && <p className="field-error" role="alert">{error}</p>}
    {!error && !loading && items.length === 0 && <p className="loading-note">
      Aún no tienes conversaciones. Visita el perfil de un desarrollador para escribirle.
    </p>}
    {items.length > 0 && <ul className="conversation-list">
      {items.map((item) => {
        const last = item.lastMessage
        return <li key={item.conversationId}>
          <Link to={`/messages/${item.participant.id}`}
            className={`conversation-item ${activeId === item.participant.id ? 'active' : ''}`}
            aria-current={activeId === item.participant.id ? 'page' : undefined}>
            <span className="profile-avatar" aria-hidden="true">{item.participant.username.charAt(0).toUpperCase()}</span>
            <span className="conversation-text">
              <strong>@{item.participant.username}</strong>
              {last && <span className="conversation-preview">{last.senderId === user?.id ? 'Tú: ' : ''}{last.content}</span>}
            </span>
            <time dateTime={item.lastMessageAt}>{formatMessageDate(item.lastMessageAt)}</time>
          </Link>
        </li>
      })}
    </ul>}
    {loading && <p className="loading-note" role="status">Cargando...</p>}
    {hasMore && !loading && <button className="text-button" type="button" onClick={() => void loadMore()}>Cargar más</button>}
  </>
}
