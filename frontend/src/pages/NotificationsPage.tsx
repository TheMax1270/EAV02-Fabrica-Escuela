import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { interactionError } from '../api/interaction'
import {
  listNotifications, markAllNotificationsRead, markNotificationRead, notifyNotificationsChanged,
  type AppNotification, type NotificationPage,
} from '../api/notification'
import SiteHeader from '../components/SiteHeader'

function describe(item: AppNotification) {
  const subject = item.targetType === 'PROJECT' ? 'tu proyecto' : 'tu publicación'
  const label = item.targetLabel ? ` «${item.targetLabel}»` : ''
  switch (item.type) {
    case 'COMMENT': return `comentó ${subject}${label}`
    case 'LIKE': return `le dio Me gusta a ${subject}${label}`
    case 'FOLLOWER': return 'empezó a seguirte'
    case 'MESSAGE': return 'te envió un mensaje'
  }
}

function destination(item: AppNotification) {
  if (item.type === 'MESSAGE') return `/messages/${item.actorId}`
  if (item.type === 'FOLLOWER') return `/developers/${item.actorId}`
  return item.targetType === 'PROJECT' ? `/projects/${item.targetId}` : '/posts'
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('es-CO', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

export default function NotificationsPage() {
  const [data, setData] = useState<NotificationPage | null>(null)
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    listNotifications(page, controller.signal).then((result) => {
      if (!controller.signal.aborted) { setData(result); setError(null) }
    }).catch((failure: unknown) => {
      if (!controller.signal.aborted) setError(interactionError(failure, 'No pudimos cargar tus notificaciones.'))
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [page, reload])

  function goToPage(next: number) { setLoading(true); setPage(next) }
  function retry() { setLoading(true); setError(null); setReload((value) => value + 1) }

  function setRead(id: string, read: boolean) {
    setData((current) => current
      ? { ...current, content: current.content.map((row) => (row.id === id ? { ...row, read } : row)) }
      : current)
  }

  async function open(item: AppNotification) {
    if (item.read) return
    setRead(item.id, true)
    try {
      await markNotificationRead(item.id)
      notifyNotificationsChanged()
    } catch {
      setRead(item.id, false)
    }
  }

  async function markAll() {
    if (busy) return
    setBusy(true)
    setActionError(null)
    try {
      await markAllNotificationsRead()
      setData((current) => current
        ? { ...current, content: current.content.map((row) => ({ ...row, read: true })) }
        : current)
      notifyNotificationsChanged()
    } catch (failure: unknown) {
      setActionError(interactionError(failure, 'No pudimos marcar las notificaciones como leídas.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="account-main content-width notifications-main">
        <div className="page-heading">
          <p className="eyebrow">ACTIVIDAD</p>
          <h1>Notificaciones</h1>
          <p>Entérate de lo que pasa con tu contenido y tu red sin revisar cada módulo.</p>
        </div>

        {loading && <p className="loading-note" role="status">Cargando notificaciones...</p>}
        {error && <div className="form-error" role="alert"><span>{error}</span>
          <button className="text-button" type="button" onClick={retry}>Reintentar</button></div>}
        {actionError && <p className="form-error" role="alert">{actionError}</p>}

        {!error && data && <>
          <div className="notifications-toolbar">
            <p className="admin-count" role="status">
              {data.totalElements} {data.totalElements === 1 ? 'notificación' : 'notificaciones'}
            </p>
            {data.content.length > 0 && <button className="button button-secondary" type="button"
              disabled={busy} onClick={() => void markAll()}>
              {busy ? 'Marcando...' : 'Marcar todas como leídas'}
            </button>}
          </div>

          {data.content.length === 0 && !loading && <p className="interaction-empty">
            Aún no tienes notificaciones. Aparecerán cuando alguien interactúe con tu contenido, te siga o te escriba.
          </p>}

          <ul className="notification-list">
            {data.content.map((item) => (
              <li key={item.id}>
                <Link className={`notification-item ${item.read ? '' : 'unread'}`} to={destination(item)}
                  onClick={() => void open(item)}>
                  <span className="profile-avatar" aria-hidden="true">{item.actorUsername.charAt(0).toUpperCase()}</span>
                  <span className="notification-text"><strong>@{item.actorUsername}</strong> {describe(item)}</span>
                  <time dateTime={item.createdAt}>{formatDate(item.createdAt)}</time>
                </Link>
              </li>
            ))}
          </ul>

          {data.totalPages > 1 && <nav className="pagination" aria-label="Paginación de notificaciones">
            <button className="button button-secondary" type="button" disabled={loading || page === 0}
              onClick={() => goToPage(page - 1)}>Anterior</button>
            <span>Página {page + 1} de {data.totalPages}</span>
            <button className="button button-secondary" type="button" disabled={loading || page >= data.totalPages - 1}
              onClick={() => goToPage(page + 1)}>Siguiente</button>
          </nav>}
        </>}
      </main>
    </div>
  )
}