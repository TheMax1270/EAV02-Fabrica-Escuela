import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import AdminModerationPage from '../AdminModerationPage'
import { dismissReport, listPendingReports, listResolvedReports, reviewReport } from '../../api/moderation'

vi.mock('../../api/moderation', () => ({
  dismissReport: vi.fn(), listPendingReports: vi.fn(), listResolvedReports: vi.fn(),
  removeReportedContent: vi.fn(), restoreReport: vi.fn(), reviewReport: vi.fn(),
}))
vi.mock('../../components/SiteHeader', () => ({ default: () => <header /> }))

const mockedPending = vi.mocked(listPendingReports)
const mockedResolved = vi.mocked(listResolvedReports)
const mockedReview = vi.mocked(reviewReport)
const mockedDismiss = vi.mocked(dismissReport)
const report = {
  id: 'report-1', reporterId: 'reporter', reporterUsername: 'lin', targetType: 'POST' as const,
  targetId: 'post-1', targetPreview: 'A reported post', targetUsername: 'ada', reason: 'SPAM' as const,
  details: 'Repeated links', status: 'PENDING' as const, moderationStatus: 'VISIBLE' as const,
  createdAt: '2026-01-02T10:00:00Z',
}
const reportPage = { content: [report], page: 0, size: 20, totalElements: 1, totalPages: 1 }

describe('AdminModerationPage', () => {
  beforeEach(() => {
    mockedPending.mockReset().mockResolvedValue(reportPage)
    mockedResolved.mockReset().mockResolvedValue({ ...reportPage, content: [] })
    mockedReview.mockReset().mockResolvedValue({ ...report, moderationStatus: 'IN_REVIEW' })
    mockedDismiss.mockReset().mockResolvedValue(report)
  })

  it('loads pending reports, confirms a review action, and updates the report status', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><AdminModerationPage /></MemoryRouter>)

    expect(await screen.findByText('A reported post')).toBeInTheDocument()
    expect(screen.getByText('1 reportes pendientes')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'En revisión' }))
    const dialog = screen.getByRole('dialog', { name: /enviar a revisión/i })
    await user.click(within(dialog).getByRole('button', { name: /confirmar acción/i }))

    expect(await screen.findByText('En revisión')).toBeInTheDocument()
    expect(mockedReview).toHaveBeenCalledWith('report-1')
    expect(mockedPending).toHaveBeenCalledWith(0, 20)
  })

  it('switches to resolved reports and reports loading failures with retry', async () => {
    mockedPending.mockRejectedValueOnce(new Error('offline'))
    const user = userEvent.setup()
    render(<MemoryRouter><AdminModerationPage /></MemoryRouter>)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByText('A reported post')).toBeInTheDocument()
    await user.click(screen.getByRole('tab', { name: 'Resueltos' }))
    expect(await screen.findByText('No hay reportes pendientes.')).toBeInTheDocument()
    expect(mockedResolved).toHaveBeenCalledWith(0, 20)
  })
})
