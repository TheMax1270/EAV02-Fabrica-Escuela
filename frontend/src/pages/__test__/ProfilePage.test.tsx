import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import ProfilePage from '../ProfilePage'
import { createProfile, getOwnProfile } from '../../api/profile'
import { useAuth } from '../../context/useAuth'

vi.mock('../../api/profile', () => ({
  createProfile: vi.fn(), getOwnProfile: vi.fn(), updateOwnProfile: vi.fn(),
}))
vi.mock('../../context/useAuth')
vi.mock('../../components/SiteHeader', () => ({ default: () => <header /> }))

const mockedGetProfile = vi.mocked(getOwnProfile)
const mockedCreateProfile = vi.mocked(createProfile)
const profile = {
  id: 'profile-1', userId: 'user-1', username: 'ada', biography: 'I build software.',
  programmingLanguages: ['TypeScript'], technologies: ['React'], experienceLevel: 'SENIOR' as const,
  githubUrl: null, linkedinUrl: null, portfolioUrl: null,
  createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
}

describe('ProfilePage', () => {
  beforeEach(() => {
    mockedGetProfile.mockReset().mockRejectedValue({ isAxiosError: true, response: { status: 404 } })
    mockedCreateProfile.mockReset().mockResolvedValue(profile)
    vi.mocked(useAuth).mockReturnValue({
      user: { id: 'user-1', username: 'ada', role: 'DEVELOPER', enabled: true },
      isAuthenticated: true, isLoggingOut: false, isRestoringSession: false,
      sessionRestoreError: null, retrySessionRestore: vi.fn(), login: vi.fn(), logout: vi.fn(), updateUser: vi.fn(),
    })
  })

  it('creates a technical profile with parsed skill lists and shows the public view', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><ProfilePage /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: 'Crear perfil técnico' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Crear perfil' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/completa la biografía/i)
    await user.type(screen.getByLabelText(/biografía corta/i), 'I build software.')
    await user.type(screen.getByLabelText(/lenguajes de programación/i), 'TypeScript, Go')
    await user.type(screen.getByLabelText(/tecnologías y frameworks/i), 'React')
    await user.selectOptions(screen.getByLabelText(/nivel de experiencia/i), 'SENIOR')
    await user.click(screen.getByRole('button', { name: 'Crear perfil' }))

    expect(await screen.findByLabelText(/vista pública del perfil técnico/i)).toHaveTextContent('I build software.')
    expect(mockedCreateProfile).toHaveBeenCalledWith({
      biography: 'I build software.', programmingLanguages: ['TypeScript', 'Go'], technologies: ['React'],
      experienceLevel: 'SENIOR', githubUrl: null, linkedinUrl: null, portfolioUrl: null,
    })
  })

  it('shows an error when an existing profile cannot be loaded', async () => {
    mockedGetProfile.mockRejectedValueOnce(new Error('offline'))
    render(<MemoryRouter><ProfilePage /></MemoryRouter>)
    expect(await screen.findByRole('alert')).toHaveTextContent(/no pudimos cargar tu perfil técnico/i)
  })
})
