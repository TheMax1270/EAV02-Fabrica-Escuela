import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ReportButton from '../ReportButton'
import { useAuth } from '../../context/useAuth'

vi.mock('../../api/report', () => ({
  reportError: (_failure: unknown, fallback: string) => fallback,
}))
vi.mock('../../context/useAuth')

const mockedUseAuth = vi.mocked(useAuth)
function auth(isAuthenticated: boolean) {
  mockedUseAuth.mockReturnValue({
    user: isAuthenticated ? { id: 'viewer', username: 'ada', role: 'DEVELOPER', enabled: true } : null,
    isAuthenticated, isLoggingOut: false, isRestoringSession: false, sessionRestoreError: null,
    retrySessionRestore: vi.fn(), login: vi.fn(), logout: vi.fn(), updateUser: vi.fn(),
  })
}

describe('ReportButton', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    auth(true)
  })

  it('hides reporting for signed-out visitors and reflects an existing report', async () => {
    auth(false)
    const hidden = render(<ReportButton onSubmit={vi.fn()} />)
    expect(screen.queryByRole('button', { name: /reportar/i })).not.toBeInTheDocument()
    hidden.unmount()

    auth(true)
    const statusCheck = vi.fn().mockResolvedValue(true)
    render(<ReportButton onSubmit={vi.fn()} statusCheck={statusCheck} />)
    expect(await screen.findByText('Reportado · en revisión')).toBeInTheDocument()
    expect(statusCheck).toHaveBeenCalledOnce()
    expect(screen.queryByRole('button', { name: /reportar/i })).not.toBeInTheDocument()
  })

  it('submits the selected reason and trimmed details, then confirms success', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<ReportButton onSubmit={onSubmit} />)
    await user.click(screen.getByRole('button', { name: /reportar/i }))
    await user.selectOptions(screen.getByLabelText('Motivo'), 'HARASSMENT')
    await user.type(screen.getByLabelText(/detalles/i), '  Repeated abuse  ')
    await user.click(screen.getByRole('button', { name: 'Enviar reporte' }))

    expect(onSubmit).toHaveBeenCalledWith({ reason: 'HARASSMENT', details: 'Repeated abuse' })
    expect(await screen.findByText('Reportado · en revisión')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('keeps the dialog open and shows an error when submission fails', async () => {
    const onSubmit = vi.fn().mockRejectedValue(new Error('failed'))
    const user = userEvent.setup()
    render(<ReportButton onSubmit={onSubmit} />)
    await user.click(screen.getByRole('button', { name: /reportar/i }))
    await user.click(screen.getByRole('button', { name: 'Enviar reporte' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos enviar el reporte.')
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })
})
