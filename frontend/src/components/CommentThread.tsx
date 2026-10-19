import { useEffect, useState } from 'react'
import { listComments, interactionError, type CommentPage } from '../api/interaction'
import CommentForm from './CommentForm'
import CommentItem from './CommentItem'

export default function CommentThread({ path, replies = false, onCreated, onDeleted }: {
  path: string; replies?: boolean; onCreated?: () => void; onDeleted?: () => void
}) {
  const [data, setData] = useState<CommentPage | null>(null)
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [composerOpen, setComposerOpen] = useState(false)
  useEffect(() => {
    const controller = new AbortController()
    listComments(path, page, controller.signal).then((result) => {
      if (!controller.signal.aborted) { setData(result); setError(null) }
    }).catch((failure: unknown) => {
      if (!controller.signal.aborted) setError(interactionError(failure, 'No pudimos cargar los comentarios.'))
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [path, page, reload])
  function refresh(nextPage = page) { setLoading(true); setError(null); setPage(nextPage); setReload((value) => value + 1) }
  function created() {
    setSuccess(true)
    setComposerOpen(false)
    refresh(data ? Math.floor(data.totalElements / 20) : 0)
    onCreated?.()
  }
  function deleted(commentId: string) {
    setData((current) => current ? {
      ...current,
      content: current.content.filter((comment) => comment.id !== commentId),
      totalElements: Math.max(0, current.totalElements - 1),
    } : current)
    onDeleted?.()
  }
  return <div className={replies ? 'reply-thread' : 'comment-thread'}>
    {!composerOpen && <button className="comment-box-trigger" type="button" onClick={() => setComposerOpen(true)}>
      <span aria-hidden="true">◌</span> {replies ? 'Responder a este comentario' : 'Añadir comentario'}
    </button>}
    {composerOpen && <div className="comment-box">
      <CommentForm path={path} reply={replies} onCreated={created} onCancel={() => setComposerOpen(false)} />
    </div>}
    {success && <p className="post-success" role="status">{replies ? 'Respuesta enviada.' : 'Comentario enviado.'}</p>}
    {loading && <p className="loading-note" role="status">Cargando {replies ? 'respuestas' : 'comentarios'}...</p>}
    {error && <div className="form-error" role="alert"><p>{error}</p><button className="text-button" onClick={() => refresh()} type="button">Reintentar</button></div>}
    {!loading && !error && data && <>
      {data.content.length === 0 && <p className="interaction-empty">{replies ? 'Aún no hay respuestas.' : 'Aún no hay comentarios. Comparte tu primera impresión.'}</p>}
      {data.content.map((comment) => <CommentItem key={comment.id} comment={comment} allowReplies
        onDeleted={() => deleted(comment.id)} />)}
      {data.totalPages > 1 && <nav className="pagination" aria-label={replies ? 'Páginas de respuestas' : 'Páginas de comentarios'}>
        <button type="button" className="button button-secondary" disabled={page === 0} onClick={() => refresh(page - 1)}>Anterior</button>
        <span>{data.page + 1} / {data.totalPages}</span>
        <button type="button" className="button button-secondary" disabled={page >= data.totalPages - 1} onClick={() => refresh(page + 1)}>Siguiente</button>
      </nav>}
    </>}
  </div>
}
