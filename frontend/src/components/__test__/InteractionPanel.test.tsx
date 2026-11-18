import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import InteractionPanel from '../InteractionPanel'
import { getCommentCount, getLikes } from '../../api/interaction'
import { useAuth } from '../../context/useAuth'

vi.mock('../../api/interaction', () => ({
  getCommentCount: vi.fn(),
  getLikes: vi.fn(),
}))
vi.mock('../../context/useAuth')
vi.mock('../LikeButton', () => ({
  default: ({ target }: { target: { kind: string; id: string } }) =>
    <button>Like {target.kind} {target.id}</button>,
}))
vi.mock('../CommentsPanel', () => ({
  default: ({ target }: { target: { kind: string; id: string } }) =>
    <div>Comments for {target.kind} {target.id}</div>,
}))

const mockedGetCommentCount = vi.mocked(getCommentCount)
const mockedGetLikes = vi.mocked(getLikes)
const mockedUseAuth = vi.mocked(useAuth)
const target = { kind: 'posts' as const, id: 'post-9' }

describe('InteractionPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedUseAuth.mockReturnValue({
      user: null, isAuthenticated: false, isLoggingOut: false, isRestoringSession: false,
      sessionRestoreError: null, retrySessionRestore: vi.fn(), login: vi.fn(), logout: vi.fn(), updateUser: vi.fn(),
    })
    mockedGetLikes.mockResolvedValue({ likeCount: 4, likedByMe: null, usernames: [] })
    mockedGetCommentCount.mockResolvedValue(3)
  })

  it('loads aggregate counts and expands/collapses the interaction controls', async () => {
    const user = userEvent.setup()
    render(<InteractionPanel target={target} />)

    expect(await screen.findByRole('button', { name: /4 me gusta/i })).toHaveAttribute('aria-expanded', 'false')
    expect(screen.getByRole('button', { name: /3 comentarios/i })).toBeInTheDocument()
    expect(mockedGetLikes).toHaveBeenCalledWith(target)
    expect(mockedGetCommentCount).toHaveBeenCalledWith(target)
    await user.click(screen.getByRole('button', { name: /3 comentarios/i }))
    expect(screen.getByRole('button', { name: /like posts post-9/i })).toBeInTheDocument()
    expect(screen.getByText('Comments for posts post-9')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Ocultar interacción' }))
    expect(screen.queryByText('Comments for posts post-9')).not.toBeInTheDocument()
  })

  it('keeps the interaction panel usable when count requests fail', async () => {
    mockedGetLikes.mockRejectedValue(new Error('offline'))
    mockedGetCommentCount.mockRejectedValue(new Error('offline'))
    render(<InteractionPanel target={target} />)
    expect(await screen.findByRole('button', { name: /0 me gusta/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /0 comentarios/i })).toBeInTheDocument()
  })
})
