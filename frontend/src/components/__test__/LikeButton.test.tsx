import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import LikeButton from '../LikeButton'
import { getLikes, setLike } from '../../api/interaction'
import { useAuth } from '../../context/useAuth'

vi.mock('../../api/interaction', () => ({
  getLikes: vi.fn(),
  setLike: vi.fn(),
  interactionError: (_failure: unknown, fallback: string) => fallback,
}))
vi.mock('../../context/useAuth')

const mockedGetLikes = vi.mocked(getLikes)
const mockedSetLike = vi.mocked(setLike)
const mockedUseAuth = vi.mocked(useAuth)
const target = { kind: 'posts' as const, id: 'post-1' }

describe('LikeButton', () => {
  beforeEach(() => {
    mockedGetLikes.mockReset()
    mockedSetLike.mockReset()
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

  it('loads reaction state and updates it when the user likes a post', async () => {
    mockedGetLikes
      .mockResolvedValueOnce({ likeCount: 2, likedByMe: false, usernames: ['grace', 'alan'] })
      .mockResolvedValueOnce({ likeCount: 3, likedByMe: true, usernames: ['grace', 'alan', 'ada'] })
    mockedSetLike.mockResolvedValue(undefined)
    render(<MemoryRouter><LikeButton target={target} /></MemoryRouter>)

    const likeButton = await screen.findByRole('button', { name: /^me gusta$/i })
    expect(likeButton).toHaveAttribute('aria-pressed', 'false')
    await userEvent.setup().click(likeButton)

    await waitFor(() => expect(mockedSetLike).toHaveBeenCalledWith(target, true))
    expect(await screen.findByRole('button', { name: /te gusta/i })).toHaveAttribute('aria-pressed', 'true')
  })

  it('prompts unauthenticated visitors to sign in before reacting', async () => {
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
    mockedGetLikes.mockResolvedValue({ likeCount: 1, likedByMe: null, usernames: ['grace'] })
    render(<MemoryRouter><LikeButton target={target} /></MemoryRouter>)

    expect(await screen.findByRole('link', { name: /inicia sesión para reaccionar/i }))
      .toHaveAttribute('href', '/login')
    expect(screen.queryByRole('button', { name: /^me gusta$/i })).not.toBeInTheDocument()
  })

  it('offers a retry when loading reactions fails', async () => {
    mockedGetLikes.mockRejectedValue(new Error('Network error'))
    render(<MemoryRouter><LikeButton target={target} /></MemoryRouter>)

    expect(await screen.findByRole('alert')).toHaveTextContent(/no pudimos cargar los me gusta/i)
    expect(mockedGetLikes).toHaveBeenCalledOnce()

    await userEvent.setup().click(screen.getByRole('button', { name: /reintentar/i }))

    await waitFor(() => expect(mockedGetLikes).toHaveBeenCalledTimes(2))
  })
})
