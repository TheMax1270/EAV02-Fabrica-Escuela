import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import AdminUsersPage from '../AdminUsersPage'
import { listUsers, suspendUser } from '../../api/admin'
import { useAuth } from '../../context/useAuth'

vi.mock('../../api/admin', () => ({
  listUsers: vi.fn(), reactivateUser: vi.fn(), suspendUser: vi.fn(),
}))
vi.mock('../../context/useAuth')
vi.mock('../../components/SiteHeader', () => ({ default: () => <header /> }))

const mockedListUsers = vi.mocked(listUsers)
const mockedSuspendUser = vi.mocked(suspendUser)
const row = {
  id: 'user-2', fullName: 'Ada Lovelace', username: 'ada', email: 'ada@example.com',
  role: 'DEVELOPER' as const, status: 'ACTIVE' as const, createdAt: '2026-01-01T00:00:00Z',
}
const userPage = { content: [row], page: 0, size: 20, totalElements: 1, totalPages: 1 }

describe('AdminUsersPage', () => {
  beforeEach(() => {
    mockedListUsers.mockReset().mockResolvedValue(userPage)
    mockedSuspendUser.mockReset().mockResolvedValue({ ...row, status: 'SUSPENDED' })
    vi.mocked(useAuth).mockReturnValue({
      user: { id: 'admin-1', username: 'admin', role: 'ADMIN', enabled: true },
      isAuthenticated: true, isLoggingOut: false, isRestoringSession: false,
      sessionRestoreError: null, retrySessionRestore: vi.fn(), login: vi.fn(), logout: vi.fn(), updateUser: vi.fn(),
    })
  })

  it('lists users, applies a status filter, and confirms account suspension', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>)

    expect(await screen.findByRole('link', { name: 'ada' })).toHaveAttribute('href', '/developers/user-2')
    expect(screen.getByText('Ada Lovelace')).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Estado'), 'SUSPENDED')
    expect(await screen.findByRole('link', { name: 'ada' })).toBeInTheDocument()
    expect(mockedListUsers).toHaveBeenLastCalledWith({ page: 0, size: 20, status: 'SUSPENDED' })

    await user.click(screen.getByRole('button', { name: 'Suspender' }))
    const dialog = screen.getByRole('dialog', { name: /suspender cuenta/i })
    await user.click(within(dialog).getByRole('button', { name: 'Suspender' }))
    expect(await screen.findByText(/la cuenta de ada fue suspendida/i)).toBeInTheDocument()
    expect(mockedSuspendUser).toHaveBeenCalledWith('user-2')
  })

  it('shows a recoverable error when the user list cannot be loaded', async () => {
    mockedListUsers.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(userPage)
    const user = userEvent.setup()
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>)

    expect(await screen.findByRole('alert')).toHaveTextContent(/no pudimos cargar los usuarios/i)
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('link', { name: 'ada' })).toBeInTheDocument()
  })
})
