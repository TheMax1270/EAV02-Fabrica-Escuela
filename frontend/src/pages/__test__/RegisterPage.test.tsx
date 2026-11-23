import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import axios from 'axios'
import RegisterPage from '../RegisterPage'
import { registerUser } from '../../api/auth'
import { useAuth } from '../../context/useAuth'

vi.mock('../../api/auth', () => ({
  registerUser: vi.fn(),
}))
vi.mock('../../context/useAuth')

const mockedRegisterUser = vi.mocked(registerUser)
const mockedUseAuth = vi.mocked(useAuth)

function renderRegisterPage() {
  return render(
    <MemoryRouter>
      <RegisterPage />
    </MemoryRouter>,
  )
}

async function fillValidRegistration(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/nombre completo/i), ' Ada Lovelace ')
  await user.type(screen.getByLabelText(/nombre de usuario/i), ' ada ')
  await user.type(screen.getByLabelText(/correo electrónico/i), ' ada@example.com ')
  await user.type(screen.getByLabelText(/^contraseña$/i), 'secret123')
  await user.type(screen.getByLabelText(/confirmar contraseña/i), 'secret123')
}

describe('RegisterPage', () => {
  beforeEach(() => {
    mockedRegisterUser.mockReset()
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

  it('shows required-field errors and does not call the API for an empty form', async () => {
    const user = userEvent.setup()
    renderRegisterPage()

    await user.click(screen.getByRole('button', { name: /registrarse/i }))

    expect(await screen.findByText(/ingresa tu nombre completo/i)).toBeInTheDocument()
    expect(screen.getByText(/ingresa un nombre de usuario/i)).toBeInTheDocument()
    expect(screen.getByText(/ingresa tu correo electrónico/i)).toBeInTheDocument()
    expect(screen.getByText(/ingresa una contraseña/i)).toBeInTheDocument()
    expect(screen.getByText(/confirma tu contraseña/i)).toBeInTheDocument()
    expect(mockedRegisterUser).not.toHaveBeenCalled()
  })

  it('validates email and password rules before sending the request', async () => {
    const user = userEvent.setup()
    renderRegisterPage()
    await user.type(screen.getByLabelText(/nombre completo/i), 'Ada Lovelace')
    await user.type(screen.getByLabelText(/nombre de usuario/i), 'ada')
    await user.type(screen.getByLabelText(/correo electrónico/i), 'correo-inválido')
    await user.type(screen.getByLabelText(/^contraseña$/i), 'short')
    await user.type(screen.getByLabelText(/confirmar contraseña/i), 'otra')

    await user.click(screen.getByRole('button', { name: /registrarse/i }))

    expect(await screen.findByText(/correo electrónico válido/i)).toBeInTheDocument()
    expect(screen.getByText(/al menos 8 caracteres/i)).toBeInTheDocument()
    expect(screen.getByText(/contraseñas no coinciden/i)).toBeInTheDocument()
    expect(mockedRegisterUser).not.toHaveBeenCalled()
  })

  it('submits trimmed identity fields and shows the created username', async () => {
    mockedRegisterUser.mockResolvedValue({
      id: '1',
      fullName: 'Ada Lovelace',
      username: 'ada',
      email: 'ada@example.com',
      role: 'DEVELOPER',
      enabled: true,
      createdAt: '2026-01-01T00:00:00Z',
    })
    const user = userEvent.setup()
    renderRegisterPage()
    await fillValidRegistration(user)

    await user.click(screen.getByRole('button', { name: /registrarse/i }))

    await waitFor(() => {
      expect(mockedRegisterUser).toHaveBeenCalledWith({
        fullName: 'Ada Lovelace',
        username: 'ada',
        email: 'ada@example.com',
        password: 'secret123',
        passwordConfirmation: 'secret123',
      })
    })
    expect(await screen.findByRole('status')).toHaveTextContent(/cuenta creada para ada/i)
    expect(screen.getByLabelText(/^contraseña$/i)).toHaveValue('')
    expect(screen.getByLabelText(/confirmar contraseña/i)).toHaveValue('')
  })

  it('shows backend field errors for a rejected registration', async () => {
    mockedRegisterUser.mockRejectedValue({
      isAxiosError: true,
      response: { status: 400, data: { errors: { username: 'Ese usuario no está disponible.' } } },
    })
    vi.spyOn(axios, 'isAxiosError').mockReturnValue(true)
    const user = userEvent.setup()
    renderRegisterPage()
    await fillValidRegistration(user)

    await user.click(screen.getByRole('button', { name: /registrarse/i }))

    expect(await screen.findByText(/ese usuario no está disponible/i)).toBeInTheDocument()
  })

  it('shows a general form error when registration cannot be completed', async () => {
    mockedRegisterUser.mockRejectedValue(new Error('Network error'))
    const user = userEvent.setup()
    renderRegisterPage()
    await fillValidRegistration(user)

    await user.click(screen.getByRole('button', { name: /registrarse/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/no pudimos completar el registro/i)
  })

  it('marks invalid fields accessibly and clears an error when the field changes', async () => {
    renderRegisterPage()
    fireEvent.submit(screen.getByRole('button', { name: /registrarse/i }).closest('form')!)
    const fullName = screen.getByLabelText(/nombre completo/i)

    expect(await screen.findByText(/ingresa tu nombre completo/i)).toBeInTheDocument()
    expect(fullName).toHaveAttribute('aria-invalid', 'true')

    await userEvent.setup().type(fullName, 'Ada Lovelace')

    expect(screen.queryByText(/ingresa tu nombre completo/i)).not.toBeInTheDocument()
    expect(fullName).toHaveAttribute('aria-invalid', 'false')
  })
})
