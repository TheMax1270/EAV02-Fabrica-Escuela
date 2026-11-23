import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import SiteHeader from '../SiteHeader'
import { useAuth } from '../../context/useAuth'

vi.mock('../../context/useAuth')

const mockedUseAuth = vi.mocked(useAuth)
const logout = vi.fn()

function setup(options: { authenticated?: boolean; role?: 'DEVELOPER' | 'ADMIN'; loggingOut?: boolean } = {}) {
  const authenticated = options.authenticated ?? false
  mockedUseAuth.mockReturnValue({
    user: authenticated ? { id: 'user-1', username: 'ada', role: options.role ?? 'DEVELOPER', enabled: true } : null,
    isAuthenticated: authenticated, isLoggingOut: options.loggingOut ?? false, isRestoringSession: false,
    sessionRestoreError: null, retrySessionRestore: vi.fn(), login: vi.fn(), logout, updateUser: vi.fn(),
  })
  return render(<MemoryRouter><SiteHeader /></MemoryRouter>)
}

describe('SiteHeader', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows public navigation and authentication links to guests', () => {
    setup()

    expect(screen.getByRole('link', { name: 'Inicio' })).toHaveAttribute('href', '/')
    expect(screen.getByRole('link', { name: 'Publicaciones' })).toHaveAttribute('href', '/posts')
    expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toHaveAttribute('href', '/login')
    expect(screen.getByRole('link', { name: 'Crear cuenta' })).toHaveAttribute('href', '/register')
    expect(screen.queryByRole('button', { name: /cerrar sesión/i })).not.toBeInTheDocument()
  })

  it('shows account navigation and invokes logout for an authenticated developer', async () => {
    const user = userEvent.setup()
    setup({ authenticated: true })

    expect(screen.getByText('ada')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Mi cuenta' })).toHaveAttribute('href', '/account')
    expect(screen.getByRole('link', { name: 'Perfil técnico' })).toHaveAttribute('href', '/profile')
    expect(screen.queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Cerrar sesión' }))
    expect(logout).toHaveBeenCalledOnce()
  })

  it('adds admin destinations and disables logout while it is in progress', () => {
    setup({ authenticated: true, role: 'ADMIN', loggingOut: true })

    expect(screen.getByRole('link', { name: 'Usuarios' })).toHaveAttribute('href', '/admin/users')
    expect(screen.getByRole('link', { name: 'Reportes' })).toHaveAttribute('href', '/admin/reports')
    expect(screen.getByRole('button', { name: 'Cerrando...' })).toBeDisabled()
  })
})
