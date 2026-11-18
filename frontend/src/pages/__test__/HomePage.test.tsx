import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import HomePage from '../HomePage'
import { useAuth } from '../../context/useAuth'

vi.mock('../../context/useAuth')

const mockedUseAuth = vi.mocked(useAuth)

describe('HomePage', () => {
  beforeEach(() => {
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
  })

  it('offers registration and login links to visitors', () => {
    render(<MemoryRouter><HomePage /></MemoryRouter>)
    const hero = screen.getByRole('region', { name: /red social para desarrolladores/i })

    expect(screen.getByRole('heading', { name: /red social para desarrolladores/i })).toBeInTheDocument()
    expect(within(hero).getByRole('link', { name: /crear cuenta/i })).toHaveAttribute('href', '/register')
    expect(within(hero).getByRole('link', { name: /iniciar sesión/i })).toHaveAttribute('href', '/login')
  })

  it('offers account navigation to authenticated users', () => {
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
    render(<MemoryRouter><HomePage /></MemoryRouter>)

    expect(screen.getByRole('link', { name: /ir a mi cuenta/i })).toHaveAttribute('href', '/account')
    expect(screen.queryByRole('link', { name: /iniciar sesión/i })).not.toBeInTheDocument()
  })
})
