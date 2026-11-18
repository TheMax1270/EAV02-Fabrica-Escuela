import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import ProtectedRoute from '../ProtectedRoute'
import { useAuth } from '../../context/useAuth'

vi.mock('../../context/useAuth')

const mockedUseAuth = vi.mocked(useAuth)

function renderProtectedRoute(role?: 'DEVELOPER' | 'ADMIN') {
  return render(
    <MemoryRouter initialEntries={['/privado']}>
      <Routes>
        <Route path="/login" element={<p>Página de inicio de sesión</p>} />
        <Route path="/privado" element={
          <ProtectedRoute role={role}>
            <p>Contenido protegido</p>
          </ProtectedRoute>
        } />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  const retrySessionRestore = vi.fn()
  const logout = vi.fn()

  beforeEach(() => {
    retrySessionRestore.mockReset()
    logout.mockReset()
    mockedUseAuth.mockReturnValue({
      user: { id: '1', username: 'ada', role: 'DEVELOPER', enabled: true },
      isAuthenticated: true,
      isLoggingOut: false,
      isRestoringSession: false,
      sessionRestoreError: null,
      retrySessionRestore,
      login: vi.fn(),
      logout,
      updateUser: vi.fn(),
    })
  })

  it('renders protected content for an authenticated user', () => {
    renderProtectedRoute()

    expect(screen.getByText('Contenido protegido')).toBeInTheDocument()
  })

  it('redirects unauthenticated users to login', () => {
    mockedUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoggingOut: false,
      isRestoringSession: false,
      sessionRestoreError: null,
      retrySessionRestore,
      login: vi.fn(),
      logout,
      updateUser: vi.fn(),
    })
    renderProtectedRoute()

    expect(screen.getByText('Página de inicio de sesión')).toBeInTheDocument()
  })

  it('shows a loading state while restoring the session', () => {
    mockedUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoggingOut: false,
      isRestoringSession: true,
      sessionRestoreError: null,
      retrySessionRestore,
      login: vi.fn(),
      logout,
      updateUser: vi.fn(),
    })
    renderProtectedRoute()

    expect(screen.getByRole('status')).toHaveTextContent(/restaurando sesión/i)
  })

  it('provides retry and login actions when session restoration fails', async () => {
    mockedUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoggingOut: false,
      isRestoringSession: false,
      sessionRestoreError: 'Error al restaurar la sesión.',
      retrySessionRestore,
      login: vi.fn(),
      logout,
      updateUser: vi.fn(),
    })
    renderProtectedRoute()
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /reintentar/i }))
    await user.click(screen.getByRole('button', { name: /ir a iniciar sesión/i }))

    expect(screen.getByRole('alert')).toHaveTextContent(/error al restaurar la sesión/i)
    expect(retrySessionRestore).toHaveBeenCalledOnce()
    expect(logout).toHaveBeenCalledOnce()
  })

  it('denies access when the authenticated user has the wrong role', () => {
    renderProtectedRoute('ADMIN')

    expect(screen.queryByText('Contenido protegido')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /no tienes permisos/i })).toBeInTheDocument()
  })
})
