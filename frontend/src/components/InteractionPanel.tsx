import { useEffect, useState } from 'react'
import { getCommentCount, getLikes, type Target } from '../api/interaction'
import CommentsPanel from './CommentsPanel'
import LikeButton from './LikeButton'
import { useAuth } from '../context/useAuth'
export default function InteractionPanel({ target }: { target: Target }) {
  const [open, setOpen] = useState(false)
  const [likeCount, setLikeCount] = useState(0)
  const [commentCount, setCommentCount] = useState(0)
  const { user } = useAuth()
  useEffect(() => {
    let cancelled = false
    Promise.all([getLikes(target), getCommentCount(target)]).then(([likes, comments]) => {
      if (!cancelled) { setLikeCount(likes.likeCount); setCommentCount(comments) }
    }).catch(() => undefined)
    return () => { cancelled = true }
  }, [target.kind, target.id, user?.id])
  return <div className="interaction-panel">
    <div className="interaction-summary">
      <button className="interaction-summary-button" type="button" aria-expanded={open} onClick={() => setOpen(true)}>
        <span aria-hidden="true">♥</span> {likeCount} Me gusta
      </button>
      <button className="interaction-summary-button" type="button" aria-expanded={open} onClick={() => setOpen(true)}>
        <span aria-hidden="true">◌</span> {commentCount} Comentarios
      </button>
    </div>
    {open && <div className="interaction-content"><LikeButton key={`${target.kind}-${target.id}-${user?.id ?? 'anonymous'}`} target={target} /><CommentsPanel target={target} /></div>}
    {open && <button className="interaction-close" type="button" onClick={() => setOpen(false)}>Ocultar interacción</button>}
  </div>
}
