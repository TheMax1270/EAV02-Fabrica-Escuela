import { useState } from 'react'
import type { Comment } from '../api/interaction'
import RepliesPanel from './RepliesPanel'
import ReportButton from './ReportButton'
import { reportComment } from '../api/report'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { commentReportStatus } from '../api/report'

export default function CommentItem({ comment, allowReplies }: { comment: Comment; allowReplies: boolean }) {
  const { user } = useAuth()
  const [open, setOpen] = useState(false)
  const [count, setCount] = useState(comment.replyCount)
  return <article className="comment-item">
    <div className="comment-meta"><Link to={`/developers/${comment.userId}`}><strong>@{comment.username}</strong></Link>
      <time dateTime={comment.createdAt}>{new Date(comment.createdAt).toLocaleString('es-CO')}</time></div>
    <p className="post-content">{comment.content}</p>
    {allowReplies && <button type="button" className="text-button comment-reply-toggle" aria-expanded={open}
      onClick={() => setOpen((value) => !value)}>{open ? 'Ocultar respuestas' : `Respuestas (${count}) · Ver o responder`}</button>}
    {allowReplies && open && <RepliesPanel commentId={comment.id} onCreated={() => setCount((value) => value + 1)} />}
      {user?.id !== comment.userId && <ReportButton statusCheck={() => commentReportStatus(comment.id)}
        onSubmit={(payload) => reportComment(comment.id, payload)} />}
  </article>
}
