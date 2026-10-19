import CommentThread from './CommentThread'
export default function RepliesPanel({ commentId, onCreated, onDeleted }: {
  commentId: string; onCreated: () => void; onDeleted: () => void
}) {
  return <CommentThread path={`/api/v1/comments/${commentId}/replies`} replies onCreated={onCreated} onDeleted={onDeleted} />
}
