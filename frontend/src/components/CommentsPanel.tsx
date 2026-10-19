import { targetPath, type Target } from '../api/interaction'
import CommentThread from './CommentThread'
export default function CommentsPanel({ target }: { target: Target }) {
  return <section aria-label="Comentarios"><h4>Comentarios</h4><CommentThread path={`${targetPath(target)}/comments`} /></section>
}
