import { useState } from 'react'
import type { Target } from '../api/interaction'
import CommentsPanel from './CommentsPanel'
import LikeButton from './LikeButton'
import { useAuth } from '../context/useAuth'
export default function InteractionPanel({ target }: { target: Target }) {
  const [open, setOpen] = useState(false)
  const { user } = useAuth()
  return <div className="interaction-panel">
    <button className="text-button" type="button" aria-expanded={open} onClick={() => setOpen((value) => !value)}>
      {open ? 'Ocultar interacción' : 'Comentarios y Me gusta'}
    </button>
    {open && <div className="interaction-content"><LikeButton key={`${target.kind}-${target.id}-${user?.id ?? 'anonymous'}`} target={target} /><CommentsPanel target={target} /></div>}
  </div>
}
