import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import AccountPage from '../AccountPage'
import { getCurrentUser } from '../../api/auth'
import { useAuth } from '../../context/useAuth'

vi.mock('../../api/auth', () => ({ getCurrentUser: vi.fn() }))
vi.mock('../../context/useAuth')
vi.mock('../../components/SiteHeader', () => ({ default: () => <header /> }))
vi.mock('../../components/FollowButton', () => ({ default: () => <button type="button">Seguir</button> }))
vi.mock('../../components/FollowListPanel', () => ({ default: () => <div /> }))

const mockedGetCurrentUser = vi.mocked(getCurrentUser)
const mockedUseAuth = vi.mocked(useAuth)
const account = { id: 'user-1', username: 'ada', role: 'DEVELOPER' as const, enabled: true }

describe('AccountPage', () => {
  const logout = vi.fn()
  const updateUser = vi.fn()

  beforeEach(() => {
    mockedGetCurrentUser.mockReset()
    logout.mockReset()
    updateUser.mockReset()
    mockedUseAuth.mockReturnValue({
      user: account, isAuthenticated: true, isLoggingOut: false, isRestoringSession: false,
      sessionRestoreError: null, retrySessionRestore: vi.fn(), login: vi.fn(), logout, updateUser,
    })
  })

  it('shows a loading status then the refreshed account and supports logout', async () => {
    let resolveUser!: (value: typeof account) => void
    mockedGetCurrentUser.mockReturnValue(new Promise((resolve) => { resolveUser = resolve }))
    const user = userEvent.setup()
    render(<MemoryRouter><AccountPage /></MemoryRouter>)

    expect(screen.getByRole('status')).toHaveTextContent(/cargando cuenta/i)
    resolveUser(account)

    expect(await screen.findByRole('heading', { name: 'ada' })).toBeInTheDocument()
    expect(screen.getByText('Desarrollador', { selector: 'strong' })).toBeInTheDocument()
    expect(updateUser).toHaveBeenCalledWith(account)
    await user.click(screen.getByRole('button', { name: /cerrar sesión/i }))
    expect(logout).toHaveBeenCalledOnce()
  })

  it('lets the user retry a failed account request', async () => {
    mockedGetCurrentUser.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(account)
    const user = userEvent.setup()
    render(<MemoryRouter><AccountPage /></MemoryRouter>)

    expect(await screen.findByRole('alert')).toHaveTextContent(/no pudimos cargar tu cuenta/i)
    await user.click(screen.getByRole('button', { name: /reintentar/i }))
    expect(await screen.findByRole('heading', { name: 'ada' })).toBeInTheDocument()
    await waitFor(() => expect(mockedGetCurrentUser).toHaveBeenCalledTimes(2))
  })
})
