import '@testing-library/jest-dom/vitest'
import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import type { AuthUser } from '../../api/auth'
import { getStatistics, type StatisticsResponse } from '../../api/statistics'
import { useAuth } from '../../context/useAuth'
import AppRoutes from '../AppRoutes'
import SiteHeader from '../../components/SiteHeader'

vi.mock('../../context/useAuth')
vi.mock('../../api/statistics', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../api/statistics')>()),
  getStatistics: vi.fn(),
}))
vi.mock('../../api/notification', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../api/notification')>()),
  getUnreadCount: vi.fn().mockResolvedValue(0),
}))

const mockedUseAuth = vi.mocked(useAuth)
const mockedGetStatistics = vi.mocked(getStatistics)

const administrator: AuthUser = { id: 'admin-id', username: 'admin', role: 'ADMIN', enabled: true }
const developer: AuthUser = { id: 'developer-id', username: 'developer', role: 'DEVELOPER', enabled: true }
const statisticsResponse: StatisticsResponse = {
  startDate: '2026-11-01',
  endDate: '2026-11-30',
  groupBy: 'MONTH',
  totals: { users: 1, projects: 1, posts: 1, comments: 1 },
  series: [],
}

function setAuthenticatedUser(user: AuthUser) {
  mockedUseAuth.mockReturnValue({
    user,
    isAuthenticated: true,
    isLoggingOut: false,
    isRestoringSession: false,
    sessionRestoreError: null,
    retrySessionRestore: vi.fn().mockResolvedValue(undefined),
    login: vi.fn(),
    logout: vi.fn().mockResolvedValue(undefined),
    updateUser: vi.fn(),
  })
}

describe('HU-14 admin access and navigation', () => {
  beforeEach(() => {
    mockedGetStatistics.mockReset().mockResolvedValue(statisticsResponse)
  })

  it('denies the statistics route to developers', () => {
    setAuthenticatedUser(developer)
    render(<MemoryRouter initialEntries={['/admin/statistics']}><AppRoutes /></MemoryRouter>)

    expect(screen.getByRole('heading', { name: 'No tienes permisos para ver esta sección' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Estadísticas de la plataforma' })).not.toBeInTheDocument()
    expect(mockedGetStatistics).not.toHaveBeenCalled()
  })

  it('shows the statistics navigation link only to administrators', () => {
    setAuthenticatedUser(administrator)
    const { rerender } = render(<MemoryRouter><SiteHeader /></MemoryRouter>)
    expect(screen.getByRole('link', { name: 'Estadísticas' })).toHaveAttribute('href', '/admin/statistics')
    expect(screen.getByRole('link', { name: 'Usuarios' })).toHaveAttribute('href', '/admin/users')
    expect(screen.getByRole('link', { name: 'Reportes' })).toHaveAttribute('href', '/admin/reports')

    setAuthenticatedUser(developer)
    rerender(<MemoryRouter><SiteHeader /></MemoryRouter>)
    expect(screen.queryByRole('link', { name: 'Estadísticas' })).not.toBeInTheDocument()
  })

  it('renders the statistics page for administrators at its route', async () => {
    setAuthenticatedUser(administrator)
    render(<MemoryRouter initialEntries={['/admin/statistics']}><AppRoutes /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: 'Estadísticas de la plataforma' })).toBeInTheDocument()
    expect(mockedGetStatistics).toHaveBeenCalledOnce()
  })
})
