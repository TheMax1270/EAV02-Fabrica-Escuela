import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import PostsPage from '../PostsPage'
import { createPost, listPosts } from '../../api/post'
import { useAuth } from '../../context/useAuth'
import { useFollowedUsers } from '../../hooks/useFollowedUsers'

vi.mock('../../api/post', () => ({
  createPost: vi.fn(), deletePost: vi.fn(), listPosts: vi.fn(), resolvePost: vi.fn(),
}))
vi.mock('../../api/report', () => ({ postReportStatus: vi.fn(), reportPost: vi.fn() }))
vi.mock('../../context/useAuth')
vi.mock('../../hooks/useFollowedUsers')
vi.mock('../../components/SiteHeader', () => ({ default: () => <header /> }))
vi.mock('../../components/InteractionPanel', () => ({ default: () => <div /> }))
vi.mock('../../components/ReportButton', () => ({ default: () => <button type="button">Reportar</button> }))

const mockedListPosts = vi.mocked(listPosts)
const mockedCreatePost = vi.mocked(createPost)
const postPage = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

describe('PostsPage', () => {
  beforeEach(() => {
    mockedListPosts.mockReset().mockResolvedValue(postPage)
    mockedCreatePost.mockReset().mockResolvedValue({
      id: 'post-1', userId: 'user-1', username: 'ada', title: 'TypeScript tips',
      content: 'A helpful discussion', createdAt: '2026-01-01T00:00:00Z',
      type: 'TECHNICAL_DISCUSSION', link: null, categories: ['TypeScript'], resolved: false,
    })
    vi.mocked(useAuth).mockReturnValue({
      user: { id: 'user-1', username: 'ada', role: 'DEVELOPER', enabled: true },
      isAuthenticated: true, isLoggingOut: false, isRestoringSession: false,
      sessionRestoreError: null, retrySessionRestore: vi.fn(), login: vi.fn(), logout: vi.fn(), updateUser: vi.fn(),
    })
    vi.mocked(useFollowedUsers).mockReturnValue(new Set())
  })

  it('validates and publishes a technical discussion, refreshing the feed', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><PostsPage /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: /aún no hay publicaciones/i })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /crear publicación/i }))
    await user.selectOptions(screen.getByLabelText(/qué quieres compartir/i), 'TECHNICAL_DISCUSSION')
    await user.click(screen.getByRole('button', { name: 'Publicar' }))
    expect(await screen.findByText(/escribe un título para tu discusión/i)).toBeInTheDocument()
    expect(screen.getByText(/escribe el contenido de tu publicación/i)).toBeInTheDocument()
    expect(mockedCreatePost).not.toHaveBeenCalled()

    await user.type(screen.getByLabelText('Título'), 'TypeScript tips')
    await user.type(screen.getByLabelText('Descripción'), 'A helpful discussion')
    await user.type(screen.getByLabelText(/categorías o tecnologías/i), 'TypeScript')
    await user.click(screen.getByRole('button', { name: 'Publicar' }))
    expect(await screen.findByRole('status')).toHaveTextContent(/publicación creada correctamente/i)
    expect(mockedCreatePost).toHaveBeenCalledWith({
      type: 'TECHNICAL_DISCUSSION', title: 'TypeScript tips',
      content: 'A helpful discussion', categories: ['TypeScript'],
    })
    expect(mockedListPosts).toHaveBeenCalledTimes(2)
  })

  it('shows feed failures and retries loading', async () => {
    mockedListPosts.mockRejectedValueOnce(new Error('offline'))
    const user = userEvent.setup()
    render(<MemoryRouter><PostsPage /></MemoryRouter>)
    expect(await screen.findByRole('alert')).toHaveTextContent(/no pudimos cargar las publicaciones/i)
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('heading', { name: /aún no hay publicaciones/i })).toBeInTheDocument()
  })
})
