import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import CommentForm from '../CommentForm'
import { sendComment, type Comment } from '../../api/interaction'
import { useAuth } from '../../context/useAuth'

vi.mock('../../api/interaction', () => ({
  sendComment: vi.fn(),
  interactionError: (_failure: unknown, fallback: string) => fallback,
}))
vi.mock('../../context/useAuth')

const mockedSendComment = vi.mocked(sendComment)
const mockedUseAuth = vi.mocked(useAuth)

function renderCommentForm(props: { reply?: boolean; onCreated?: () => void; onCancel?: () => void } = {}) {
  const onCreated = props.onCreated ?? vi.fn()
  return {
    onCreated,
    ...render(
      <MemoryRouter>
        <CommentForm path="/api/v1/posts/1/comments" {...props} onCreated={onCreated} />
      </MemoryRouter>,
    ),
  }
}

describe('CommentForm', () => {
  beforeEach(() => {
    mockedSendComment.mockReset()
    mockedUseAuth.mockReturnValue({
      user: { id: '1', username: 'ada', role: 'DEVELOPER', enabled: true },
      isAuthenticated: true,
      isLoggingOut: false,
      isRestoringSession: false,
      sessionRestoreError: null,
      retrySessionRestore: vi.fn(),
      login: vi.fn(),
      logout: vi.fn(),
      updateUser: vi.fn(),
    })
  })

  it('prompts unauthenticated visitors to sign in', () => {
    mockedUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoggingOut: false,
      isRestoringSession: false,
      sessionRestoreError: null,
      retrySessionRestore: vi.fn(),
      login: vi.fn(),
      logout: vi.fn(),
      updateUser: vi.fn(),
    })
    renderCommentForm()

    expect(screen.getByRole('link', { name: /inicia sesión/i })).toHaveAttribute('href', '/login')
    expect(screen.getByText(/para comentar/i)).toBeInTheDocument()
  })

  it('validates empty input without sending a comment', async () => {
    const user = userEvent.setup()
    renderCommentForm()

    await user.click(screen.getByRole('button', { name: /^comentar$/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/entre 1 y 2\.000 caracteres/i)
    expect(mockedSendComment).not.toHaveBeenCalled()
  })

  it('trims and submits the comment, then notifies the parent', async () => {
    mockedSendComment.mockResolvedValue({
      id: 'comment-1',
      userId: '1',
      username: 'ada',
      projectId: null,
      postId: '1',
      parentCommentId: null,
      content: '¡Hola comunidad!',
      createdAt: '2026-01-01T00:00:00Z',
      replyCount: 0,
    } satisfies Comment)
    const user = userEvent.setup()
    const { onCreated } = renderCommentForm()
    const textarea = screen.getByLabelText(/tu comentario/i)
    await user.type(textarea, '  ¡Hola comunidad!  ')

    await user.click(screen.getByRole('button', { name: /^comentar$/i }))

    expect(mockedSendComment).toHaveBeenCalledWith('/api/v1/posts/1/comments', '¡Hola comunidad!')
    expect(onCreated).toHaveBeenCalledOnce()
    expect(textarea).toHaveValue('')
  })

  it('preserves the comment and displays an error when sending fails', async () => {
    mockedSendComment.mockRejectedValue(new Error('Request failed'))
    const user = userEvent.setup()
    renderCommentForm()
    const textarea = screen.getByLabelText(/tu comentario/i)
    await user.type(textarea, 'Mi comentario')

    await user.click(screen.getByRole('button', { name: /^comentar$/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/no pudimos enviar tu comentario/i)
    expect(textarea).toHaveValue('Mi comentario')
  })
})
