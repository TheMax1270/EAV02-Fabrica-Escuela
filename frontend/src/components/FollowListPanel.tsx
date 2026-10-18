import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listFollows, type FollowListKind, type FollowPage, type FollowUser } from '../api/follow'
import { interactionError } from '../api/interaction'

const tabLabels: Record<FollowListKind, string> = { followers: 'Seguidores', following: 'Siguiendo' }
const emptyLabels: Record<FollowListKind, string> = {
  followers: 'Aún no tiene seguidores.', following: 'Aún no sigue a nadie.',
}

export default function FollowListPanel({ userId, reloadKey }: Readonly<{ userId: string; reloadKey: number }>) {
  const [kind, setKind] = useState<FollowListKind>('followers')
  return <section className="public-profile-section follow-list-panel">
    <div className="moderation-tabs" role="tablist" aria-label="Relaciones de seguimiento">
      {(Object.keys(tabLabels) as FollowListKind[]).map((tab) => <button key={tab} type="button" role="tab"
        aria-selected={kind === tab} className={`moderation-tab ${kind === tab ? 'active' : ''}`} onClick={() => setKind(tab)}>
        {tabLabels[tab]}
      </button>)}
    </div>
    <FollowList key={`${userId}-${kind}-${reloadKey}`} userId={userId} kind={kind} />
  </section>
}

function FollowList({ userId, kind }: Readonly<{ userId: string; kind: FollowListKind }>) {
  const [items, setItems] = useState<FollowUser[]>([])
  const [page, setPage] = useState<FollowPage | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  useEffect(() => {
    const controller = new AbortController()
    listFollows(userId, kind, 0, controller.signal).then((result) => {
      if (!controller.signal.aborted) { setItems(result.content); setPage(result) }
    }).catch((failure: unknown) => {
      if (!controller.signal.aborted) setError(interactionError(failure, 'No pudimos cargar el listado.'))
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [userId, kind])
  async function loadMore() {
    if (!page) return
    setLoading(true)
    try {
      const result = await listFollows(userId, kind, page.page + 1)
      setItems((current) => [...current, ...result.content]); setPage(result)
    } catch (failure: unknown) {
      setError(interactionError(failure, 'No pudimos cargar más usuarios.'))
    } finally { setLoading(false) }
  }
  const hasMore = page !== null && page.page + 1 < page.totalPages
  return <>
    {error && <p className="field-error" role="alert">{error}</p>}
    {!error && !loading && items.length === 0 && <p className="loading-note">{emptyLabels[kind]}</p>}
    {items.length > 0 && <ul className="follow-list">
      {items.map((item) => <li key={item.userId}><Link to={`/developers/${item.userId}`}>@{item.username}</Link></li>)}
    </ul>}
    {loading && <p className="loading-note" role="status">Cargando...</p>}
    {hasMore && !loading && <button className="text-button" type="button" onClick={() => void loadMore()}>Cargar más</button>}
  </>
}
