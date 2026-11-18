import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import CommentItem from '../CommentItem'
import { deleteComment, getReplyCount, type Comment } from '../../api/interaction'
import { useAuth } from '../../context/useAuth'

vi.mock('../../api/interaction', () => ({
  deleteComment: vi.fn(),
  getReplyCount: vi.fn(),
  interactionError: (_failure: unknown, fallback: string) => fallback,
}))
vi.mock('../../api/report', () => ({
  commentReportStatus: vi.fn().mockResolvedValue(false),
  reportComment: vi.fn(),
}))
vi.mock('../../context/useAuth')
vi.mock('../RepliesPanel', () => ({
  default: ({ commentId, onCreated, onDeleted }: {
    commentId: string; onCreated: () => void; onDeleted: () => void
  }) => <div data-testid="replies-panel">
    Replies for {commentId}
    <button onClick={onCreated}>Create reply</button>
    <button onClick={onDeleted}>Delete reply</button>
  </div>,
}))

const mockedUseAuth = vi.mocked(useAuth)
const mockedDeleteComment = vi.mocked(deleteComment)
const mockedGetReplyCount = vi.mocked(getReplyCount)
const comment: Comment = {
  id: 'comment-1', userId: 'owner-1', username: 'ada', projectId: null, postId: 'post-1',
  parentCommentId: null, content: 'A useful comment', createdAt: '2026-01-01T00:00:00Z', replyCount: 2,
}

function authAs(id: string | null) {
  mockedUseAuth.mockReturnValue({
    user: id ? { id, username: 'ada', role: 'DEVELOPER', enabled: true } : null,
    isAuthenticated: Boolean(id), isLoggingOut: false, isRestoringSession: false,
    sessionRestoreError: null, retrySessionRestore: vi.fn(), login: vi.fn(), logout: vi.fn(), updateUser: vi.fn(),
  })
}

function renderItem(props: Partial<React.ComponentProps<typeof CommentItem>> = {}) {
  return render(<MemoryRouter><CommentItem comment={comment} allowReplies {...props} /></MemoryRouter>)
}

describe('CommentItem', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authAs('owner-1')
    mockedDeleteComment.mockResolvedValue()
    mockedGetReplyCount.mockResolvedValue(1)
  })

  it('shows comment author/content and updates the reply count after reply actions', async () => {
    const user = userEvent.setup()
    renderItem()

    expect(screen.getByRole('link', { name: '@ada' })).toHaveAttribute('href', '/developers/owner-1')
    expect(screen.getByText('A useful comment')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /respuestas \(2\)/i }))
    expect(screen.getByTestId('replies-panel')).toHaveTextContent('comment-1')
    await user.click(screen.getByRole('button', { name: 'Create reply' }))
    expect(screen.getByRole('button', { name: /ocultar respuestas/i })).toHaveAttribute('aria-expanded', 'true')
    await user.click(screen.getByRole('button', { name: /ocultar respuestas/i }))
    expect(await screen.findByRole('button', { name: /respuestas \(3\)/i })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /respuestas \(3\)/i }))
    await user.click(screen.getByRole('button', { name: 'Delete reply' }))
    expect(mockedGetReplyCount).toHaveBeenCalledWith('comment-1')
    await user.click(screen.getByRole('button', { name: /ocultar respuestas/i }))
    expect(await screen.findByRole('button', { name: /respuestas \(1\)/i })).toBeInTheDocument()
  })

  it('asks the owner to confirm deletion and notifies the parent after success', async () => {
    const user = userEvent.setup()
    const onDeleted = vi.fn()
    renderItem({ onDeleted })

    await user.click(screen.getByRole('button', { name: 'Eliminar' }))
    expect(screen.getByRole('dialog')).toHaveTextContent('¿Eliminar este comentario?')
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(mockedDeleteComment).not.toHaveBeenCalled()
    await user.click(screen.getByRole('button', { name: 'Eliminar' }))
    await user.click(screen.getByRole('button', { name: 'Eliminar comentario' }))

    expect(mockedDeleteComment).toHaveBeenCalledWith('comment-1')
    expect(onDeleted).toHaveBeenCalledOnce()
  })

  it('shows deletion failures and offers reporting instead of owner actions to other users', async () => {
    mockedDeleteComment.mockRejectedValue(new Error('failed'))
    const user = userEvent.setup()
    const { unmount } = renderItem()
    await user.click(screen.getByRole('button', { name: 'Eliminar' }))
    await user.click(screen.getByRole('button', { name: 'Eliminar comentario' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos eliminar el comentario.')

    unmount()
    authAs('viewer-1')
    renderItem()
    expect(screen.queryByRole('button', { name: 'Eliminar' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /reportar/i })).toBeInTheDocument()
  })
})
