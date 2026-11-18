import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import FollowButton from '../FollowButton'
import { getFollowSummary, setFollowing } from '../../api/follow'
import { getBlockSummary } from '../../api/block'
import { useAuth } from '../../context/useAuth'

vi.mock('../../api/follow', () => ({
  getFollowSummary: vi.fn(),
  setFollowing: vi.fn(),
}))
vi.mock('../../api/block', () => ({
  getBlockSummary: vi.fn(),
}))
vi.mock('../../api/interaction', () => ({
  interactionError: (_failure: unknown, fallback: string) => fallback,
}))
vi.mock('../../context/useAuth')

const mockedGetFollowSummary = vi.mocked(getFollowSummary)
const mockedSetFollowing = vi.mocked(setFollowing)
const mockedGetBlockSummary = vi.mocked(getBlockSummary)
const mockedUseAuth = vi.mocked(useAuth)

describe('FollowButton', () => {
  beforeEach(() => {
    mockedGetFollowSummary.mockReset()
    mockedSetFollowing.mockReset()
    mockedGetBlockSummary.mockReset().mockResolvedValue({ blockedByMe: false, blockedMe: false })
    mockedUseAuth.mockReturnValue({
      user: { id: 'current-user', username: 'ada', role: 'DEVELOPER', enabled: true },
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

  it('follows a developer and notifies the parent after the state refreshes', async () => {
    mockedGetFollowSummary
      .mockResolvedValueOnce({ followersCount: 3, followingCount: 2, followedByMe: false })
      .mockResolvedValueOnce({ followersCount: 4, followingCount: 2, followedByMe: true })
    mockedSetFollowing.mockResolvedValue(undefined)
    const onChange = vi.fn()
    render(<MemoryRouter><FollowButton userId="developer-1" onChange={onChange} /></MemoryRouter>)

    await userEvent.setup().click(await screen.findByRole('button', { name: /^seguir$/i }))

    await waitFor(() => {
      expect(mockedSetFollowing).toHaveBeenCalledWith('developer-1', true)
      expect(onChange).toHaveBeenCalledOnce()
    })
    expect(await screen.findByRole('button', { name: /siguiendo/i })).toHaveAttribute('aria-pressed', 'true')
  })

  it('does not offer a follow action on the current user profile', async () => {
    mockedGetFollowSummary.mockResolvedValue({
      followersCount: 3,
      followingCount: 2,
      followedByMe: false,
    })
    render(<MemoryRouter><FollowButton userId="current-user" /></MemoryRouter>)

    await waitFor(() => expect(mockedGetFollowSummary).toHaveBeenCalledWith(
      'current-user',
      expect.any(AbortSignal),
    ))
    expect(screen.queryByRole('button', { name: /seguir/i })).not.toBeInTheDocument()
  })

  it('prompts unauthenticated visitors to sign in to follow', async () => {
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
    mockedGetFollowSummary.mockResolvedValue({
      followersCount: 3,
      followingCount: 2,
      followedByMe: null,
    })
    render(<MemoryRouter><FollowButton userId="developer-1" /></MemoryRouter>)

    expect(await screen.findByRole('link', { name: /inicia sesión para seguir/i }))
      .toHaveAttribute('href', '/login')
  })

  it.each([
    [{ blockedByMe: true, blockedMe: false }, /has bloqueado a este usuario/i],
    [{ blockedByMe: false, blockedMe: true }, /te ha bloqueado/i],
  ])('does not offer a follow action when blocked (%o)', async (blockSummary, message) => {
    mockedGetFollowSummary.mockResolvedValue({
      followersCount: 3,
      followingCount: 2,
      followedByMe: false,
    })
    mockedGetBlockSummary.mockResolvedValue(blockSummary)
    render(<MemoryRouter><FollowButton userId="developer-1" /></MemoryRouter>)

    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /seguir/i })).not.toBeInTheDocument()
  })
})
