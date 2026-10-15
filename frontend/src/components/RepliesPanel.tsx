import CommentThread from './CommentThread'
export default function RepliesPanel({ commentId, onCreated }: { commentId: string; onCreated: () => void }) {
  return <CommentThread path={`/api/v1/comments/${commentId}/replies`} replies onCreated={onCreated} />
}
