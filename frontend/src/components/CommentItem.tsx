import { useState } from 'react'
import { deleteComment, getReplyCount, type Comment, interactionError } from '../api/interaction'
import RepliesPanel from './RepliesPanel'
import ReportButton from './ReportButton'
import { reportComment } from '../api/report'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { commentReportStatus } from '../api/report'

export default function CommentItem({ comment, allowReplies, onDeleted }: {
  comment: Comment; allowReplies: boolean; onDeleted?: () => void
}) {
  const { user } = useAuth()
  const [open, setOpen] = useState(false)
  const [count, setCount] = useState(comment.replyCount)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)
  async function remove() {
    if (deleting) return
    setConfirmDelete(false)
    setDeleting(true)
    setDeleteError(null)
    try {
      await deleteComment(comment.id)
      onDeleted?.()
    } catch (failure) {
      setDeleteError(interactionError(failure, 'No pudimos eliminar el comentario.'))
    } finally {
      setDeleting(false)
    }
  }
  async function refreshReplyCount() {
    try {
      setCount(await getReplyCount(comment.id))
    } catch {
      setCount((value) => Math.max(0, value - 1))
    }
  }
  return <article className="comment-item">
    <div className="comment-meta"><Link to={`/developers/${comment.userId}`}><strong>@{comment.username}</strong></Link>
      <time dateTime={comment.createdAt}>{new Date(comment.createdAt).toLocaleString('es-CO')}</time></div>
    <p className="post-content">{comment.content}</p>
    {allowReplies && <button type="button" className="text-button comment-reply-toggle" aria-expanded={open}
      onClick={() => setOpen((value) => !value)}>{open ? 'Ocultar respuestas' : `Respuestas (${count}) · Ver o responder`}</button>}
    {allowReplies && open && <RepliesPanel commentId={comment.id}
      onCreated={() => setCount((value) => value + 1)} onDeleted={() => void refreshReplyCount()} />}
    {user?.id === comment.userId && <button className="comment-delete" type="button" disabled={deleting} onClick={() => setConfirmDelete(true)}>
      {deleting ? 'Eliminando...' : 'Eliminar'}
    </button>}
    {deleteError && <p className="field-error" role="alert">{deleteError}</p>}
    {confirmDelete && <div className="modal-overlay" role="presentation">
      <div className="confirm-modal" role="dialog" aria-modal="true" aria-labelledby={`delete-comment-title-${comment.id}`}>
        <p className="eyebrow">ELIMINAR COMENTARIO</p>
        <h2 id={`delete-comment-title-${comment.id}`}>¿Eliminar este comentario?</h2>
        <p>También se ocultarán sus respuestas. Esta acción no se puede deshacer.</p>
        <div className="confirm-modal-actions">
          <button className="button button-secondary" type="button" onClick={() => setConfirmDelete(false)}>Cancelar</button>
          <button className="button button-danger" type="button" onClick={() => void remove()}>Eliminar comentario</button>
        </div>
      </div>
    </div>}
      {user?.id !== comment.userId && <ReportButton statusCheck={() => commentReportStatus(comment.id)}
        onSubmit={(payload) => reportComment(comment.id, payload)} />}
  </article>
}
