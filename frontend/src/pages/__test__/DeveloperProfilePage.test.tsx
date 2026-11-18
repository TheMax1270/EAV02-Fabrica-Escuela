import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import DeveloperProfilePage from '../DeveloperProfilePage'
import { getProfileByUser } from '../../api/profile'
import { useAuth } from '../../context/useAuth'

vi.mock('../../api/profile', () => ({ getProfileByUser: vi.fn() }))
vi.mock('../../api/report', () => ({
  profileReportStatus: vi.fn(), reportProfile: vi.fn(),
}))
vi.mock('../../context/useAuth')
vi.mock('../../components/SiteHeader', () => ({ default: () => <header /> }))
vi.mock('../../components/FollowButton', () => ({
  default: ({ countsOnly }: { countsOnly?: boolean }) => countsOnly ? <div /> : <button type="button">Seguir</button>,
}))
vi.mock('../../components/FollowListPanel', () => ({ default: () => <div /> }))
vi.mock('../../components/ReportButton', () => ({ default: () => <button type="button">Reportar perfil</button> }))
vi.mock('../../components/BlockButton', () => ({ default: () => <button type="button">Bloquear usuario</button> }))

const mockedGetProfile = vi.mocked(getProfileByUser)
const profile = {
  id: 'profile-1', userId: 'user-2', username: 'ada', biography: 'I build useful software.',
  programmingLanguages: ['TypeScript'], technologies: ['React'], experienceLevel: 'SENIOR' as const,
  githubUrl: null, linkedinUrl: null, portfolioUrl: null, createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

function renderProfile() {
  return render(<MemoryRouter initialEntries={['/developers/user-2']}>
    <Routes><Route path="/developers/:userId" element={<DeveloperProfilePage />} /></Routes>
  </MemoryRouter>)
}

describe('DeveloperProfilePage', () => {
  beforeEach(() => {
    mockedGetProfile.mockReset().mockResolvedValue(profile)
    vi.mocked(useAuth).mockReturnValue({
      user: { id: 'viewer', username: 'viewer', role: 'DEVELOPER', enabled: true },
      isAuthenticated: true, isLoggingOut: false, isRestoringSession: false,
      sessionRestoreError: null, retrySessionRestore: vi.fn(), login: vi.fn(), logout: vi.fn(), updateUser: vi.fn(),
    })
    vi.stubGlobal('scrollTo', vi.fn())
  })

  it('loads a public profile and offers community actions', async () => {
    renderProfile()

    expect(await screen.findByRole('heading', { name: 'ada' })).toBeInTheDocument()
    expect(screen.getByText('I build useful software.')).toBeInTheDocument()
    expect(screen.getByText('TypeScript')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Seguir' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Enviar mensaje' })).toHaveAttribute('href', '/messages/user-2')
    await userEvent.setup().click(screen.getByRole('button', { name: /más/i }))
    expect(screen.getByRole('button', { name: 'Reportar perfil' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Bloquear usuario' })).toBeInTheDocument()
    expect(mockedGetProfile).toHaveBeenCalledWith('user-2', expect.any(AbortSignal))
  })

  it('renders a useful error when profile loading fails', async () => {
    mockedGetProfile.mockRejectedValueOnce(new Error('offline'))
    renderProfile()
    expect(await screen.findByRole('alert')).toHaveTextContent(/no pudimos cargar este perfil/i)
  })
})
