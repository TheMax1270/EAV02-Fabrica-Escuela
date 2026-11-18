import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import CommentThread from '../CommentThread'
import { listComments, type Comment, type CommentPage } from '../../api/interaction'

vi.mock('../../api/interaction', () => ({
  listComments: vi.fn(),
  interactionError: (_failure: unknown, fallback: string) => fallback,
}))
vi.mock('../CommentForm', () => ({
  default: ({ path, reply, onCreated, onCancel }: {
    path: string; reply: boolean; onCreated: () => void; onCancel: () => void
  }) => <div data-testid="comment-form">
    <span>{path} {reply ? 'reply' : 'comment'}</span>
    <button onClick={onCreated}>Submit test comment</button>
    <button onClick={onCancel}>Cancel test comment</button>
  </div>,
}))
vi.mock('../CommentItem', () => ({
  default: ({ comment, onDeleted }: { comment: Comment; onDeleted: () => void }) =>
    <div data-testid="comment-item">{comment.content}<button onClick={onDeleted}>Delete test comment</button></div>,
}))

const mockedListComments = vi.mocked(listComments)
const comment: Comment = {
  id: 'comment-1', userId: 'user-1', username: 'ada', projectId: null, postId: 'post-1',
  parentCommentId: null, content: 'Existing comment', createdAt: '2026-01-01T00:00:00Z', replyCount: 0,
}
const page = (content: Comment[], pageNumber = 0, totalPages = 1): CommentPage => ({
  content, page: pageNumber, size: 20, totalElements: content.length, totalPages,
})

describe('CommentThread', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedListComments.mockResolvedValue(page([comment]))
  })

  it('loads and renders comments, and removes a deleted item from the current page', async () => {
    const onDeleted = vi.fn()
    const user = userEvent.setup()
    render(<CommentThread path="/posts/1/comments" onDeleted={onDeleted} />)

    expect(await screen.findByText('Existing comment')).toBeInTheDocument()
    expect(mockedListComments).toHaveBeenCalledWith('/posts/1/comments', 0, expect.any(AbortSignal))
    await user.click(screen.getByRole('button', { name: 'Delete test comment' }))
    expect(screen.queryByText('Existing comment')).not.toBeInTheDocument()
    expect(onDeleted).toHaveBeenCalledOnce()
  })

  it('shows empty content and reports load failures with a working retry action', async () => {
    mockedListComments.mockResolvedValueOnce(page([]))
    const { rerender } = render(<CommentThread path="/posts/1/comments" />)
    expect(await screen.findByText(/aún no hay comentarios/i)).toBeInTheDocument()

    mockedListComments.mockRejectedValueOnce(new Error('failed')).mockResolvedValueOnce(page([]))
    rerender(<CommentThread path="/posts/2/comments" />)
    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('No pudimos cargar los comentarios.')
    await userEvent.setup().click(screen.getByRole('button', { name: 'Reintentar' }))
    await waitFor(() => expect(mockedListComments).toHaveBeenCalledTimes(3))
    expect(await screen.findByText(/aún no hay comentarios/i)).toBeInTheDocument()
  })

  it('opens and cancels the composer, and refreshes the last page after creation', async () => {
    const onCreated = vi.fn()
    mockedListComments.mockResolvedValue(page([comment], 0, 2))
    const user = userEvent.setup()
    render(<CommentThread path="/posts/1/comments" onCreated={onCreated} />)

    await user.click(await screen.findByRole('button', { name: 'Añadir comentario' }))
    expect(screen.getByTestId('comment-form')).toHaveTextContent('/posts/1/comments comment')
    await user.click(screen.getByRole('button', { name: 'Cancel test comment' }))
    expect(screen.getByRole('button', { name: 'Añadir comentario' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Añadir comentario' }))
    await user.click(screen.getByRole('button', { name: 'Submit test comment' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Comentario enviado.')
    expect(onCreated).toHaveBeenCalledOnce()
    await waitFor(() => expect(mockedListComments).toHaveBeenLastCalledWith('/posts/1/comments', 0, expect.any(AbortSignal)))
  })

  it('paginates through results', async () => {
    mockedListComments.mockResolvedValueOnce(page([comment], 0, 2))
      .mockResolvedValueOnce(page([{ ...comment, id: 'comment-2', content: 'Next page' }], 1, 2))
    const user = userEvent.setup()
    render(<CommentThread path="/posts/1/comments" />)

    await screen.findByText('Existing comment')
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(await screen.findByText('Next page')).toBeInTheDocument()
    expect(mockedListComments).toHaveBeenLastCalledWith('/posts/1/comments', 1, expect.any(AbortSignal))
  })
})
