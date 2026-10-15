import { useState } from 'react'
import type { Comment } from '../api/interaction'
import RepliesPanel from './RepliesPanel'
export default function CommentItem({ comment, allowReplies }: { comment: Comment; allowReplies: boolean }) {
  const [open, setOpen] = useState(false)
  const [count, setCount] = useState(comment.replyCount)
  return <article className="comment-item">
    <div className="comment-meta"><strong>@{comment.username}</strong>
      <time dateTime={comment.createdAt}>{new Date(comment.createdAt).toLocaleString('es-CO')}</time></div>
    <p className="post-content">{comment.content}</p>
    {allowReplies && <button type="button" className="text-button comment-reply-toggle" aria-expanded={open}
      onClick={() => setOpen((value) => !value)}>{open ? 'Ocultar respuestas' : `Respuestas (${count}) · Ver o responder`}</button>}
    {allowReplies && open && <RepliesPanel commentId={comment.id} onCreated={() => setCount((value) => value + 1)} />}
  </article>
}
