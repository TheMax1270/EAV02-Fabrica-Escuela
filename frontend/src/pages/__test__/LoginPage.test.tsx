import '@testing-library/jest-dom/vitest'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import axios from 'axios'
import LoginPage from '../LoginPage'
import { useAuth } from '../../context/useAuth'

vi.mock('../../context/useAuth')

const mockedUseAuth = vi.mocked(useAuth)

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )
}

describe('LoginPage', () => {
  const login = vi.fn()

  beforeEach(() => {
    login.mockReset()
    mockedUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoggingOut: false,
      login,
      logout: vi.fn(),
      updateUser: vi.fn(),
    })
  })

  it('shows validation errors when submitting an empty form', async () => {
    renderLoginPage()
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /iniciar sesión/i }))

    expect(await screen.findByText(/ingresa tu usuario o correo/i)).toBeInTheDocument()
    expect(screen.getByText(/ingresa tu contraseña/i)).toBeInTheDocument()
    expect(login).not.toHaveBeenCalled()
  })

  it('calls login with the trimmed identifier and password', async () => {
    login.mockResolvedValueOnce(undefined)
    renderLoginPage()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/usuario o correo/i), ' ada ')
    await user.type(screen.getByLabelText(/contraseña/i), 'secret123')
    await user.click(screen.getByRole('button', { name: /iniciar sesión/i }))

    await waitFor(() => {
      expect(login).toHaveBeenCalledWith({ identifier: 'ada', password: 'secret123' })
    })
  })

  it('shows a specific message for invalid credentials', async () => {
    login.mockRejectedValueOnce({
      isAxiosError: true,
      response: { status: 401 },
    })
    vi.spyOn(axios, 'isAxiosError').mockReturnValue(true)
    renderLoginPage()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/usuario o correo/i), 'ada')
    await user.type(screen.getByLabelText(/contraseña/i), 'wrong-pass')
    await user.click(screen.getByRole('button', { name: /iniciar sesión/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/usuario\/correo o contraseña incorrectos/i)
  })

  it('redirects to /account when the user is already authenticated', () => {
    mockedUseAuth.mockReturnValue({
      user: { id: '1', username: 'ada', role: 'DEVELOPER', enabled: true },
      isAuthenticated: true,
      isLoggingOut: false,
      login,
      logout: vi.fn(),
      updateUser: vi.fn(),
    })

    renderLoginPage()

    expect(screen.queryByRole('button', { name: /iniciar sesión/i })).not.toBeInTheDocument()
  })
})
