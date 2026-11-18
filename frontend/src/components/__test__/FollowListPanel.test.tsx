import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import FollowListPanel from '../FollowListPanel'
import { listFollows, type FollowPage } from '../../api/follow'

vi.mock('../../api/follow', () => ({ listFollows: vi.fn() }))
vi.mock('../../api/interaction', () => ({
  interactionError: (_failure: unknown, fallback: string) => fallback,
}))

const mockedListFollows = vi.mocked(listFollows)
const followPage = (names: string[], page = 0, totalPages = 1): FollowPage => ({
  content: names.map((username, index) => ({
    userId: `${username}-${index}`, username, followedAt: '2026-01-01T00:00:00Z',
  })),
  page, size: 20, totalElements: names.length, totalPages,
})
const renderPanel = (reloadKey = 0) => render(
  <MemoryRouter><FollowListPanel userId="profile-1" reloadKey={reloadKey} /></MemoryRouter>,
)

describe('FollowListPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedListFollows.mockResolvedValue(followPage(['ada']))
  })

  it('loads followers and switches to the following list', async () => {
    mockedListFollows.mockResolvedValueOnce(followPage(['ada']))
      .mockResolvedValueOnce(followPage(['grace']))
    const user = userEvent.setup()
    renderPanel()

    expect(await screen.findByRole('link', { name: '@ada' })).toHaveAttribute('href', '/developers/ada-0')
    await user.click(screen.getByRole('tab', { name: 'Siguiendo' }))
    expect(await screen.findByRole('link', { name: '@grace' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Siguiendo' })).toHaveAttribute('aria-selected', 'true')
    expect(mockedListFollows).toHaveBeenLastCalledWith('profile-1', 'following', 0, expect.any(AbortSignal))
  })

  it('shows the empty state and appends the next page when requested', async () => {
    mockedListFollows.mockResolvedValueOnce(followPage([]))
      .mockResolvedValueOnce(followPage(['ada'], 0, 2))
      .mockResolvedValueOnce(followPage(['grace'], 1, 2))
    const user = userEvent.setup()
    const { rerender } = renderPanel()
    expect(await screen.findByText('Aún no tiene seguidores.')).toBeInTheDocument()

    rerender(<MemoryRouter><FollowListPanel userId="profile-2" reloadKey={0} /></MemoryRouter>)
    expect(await screen.findByRole('link', { name: '@ada' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Cargar más' }))
    expect(await screen.findByRole('link', { name: '@grace' })).toBeInTheDocument()
    expect(mockedListFollows).toHaveBeenLastCalledWith('profile-2', 'followers', 1)
  })

  it('displays errors from the initial request and reloads when reloadKey changes', async () => {
    mockedListFollows.mockRejectedValueOnce(new Error('failed'))
    const { rerender } = renderPanel()
    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos cargar el listado.')
    rerender(<MemoryRouter><FollowListPanel userId="profile-1" reloadKey={1} /></MemoryRouter>)
    await waitFor(() => expect(mockedListFollows).toHaveBeenCalledTimes(2))
    expect(await screen.findByRole('link', { name: '@ada' })).toBeInTheDocument()
  })
})
