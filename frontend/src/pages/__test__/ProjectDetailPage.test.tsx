import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import ProjectDetailPage from '../ProjectDetailPage'
import { getProject } from '../../api/project'

vi.mock('../../api/project', () => ({
  createProject: vi.fn(), getMyProjects: vi.fn(), getProject: vi.fn(), updateProject: vi.fn(),
}))
vi.mock('../../components/SiteHeader', () => ({ default: () => <header /> }))
vi.mock('../../components/InteractionPanel', () => ({ default: () => <div aria-label="Interacciones" /> }))

const mockedGetProject = vi.mocked(getProject)
const project = {
  id: 'project-1', userId: 'user-1', username: 'ada', title: 'Compiler',
  description: 'A small compiler', technologies: ['TypeScript', 'React'], status: 'IN_PROGRESS' as const,
  repositoryUrl: null, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
}

function renderProject() {
  return render(<MemoryRouter initialEntries={['/projects/project-1']}>
    <Routes><Route path="/projects/:projectId" element={<ProjectDetailPage />} /></Routes>
  </MemoryRouter>)
}

describe('ProjectDetailPage', () => {
  beforeEach(() => {
    mockedGetProject.mockReset().mockResolvedValue(project)
  })

  it('renders project details, technologies, and the community link', async () => {
    renderProject()
    expect(await screen.findByRole('heading', { name: 'Compiler' })).toBeInTheDocument()
    expect(screen.getByText('@ada')).toBeInTheDocument()
    expect(screen.getByText('A small compiler')).toBeInTheDocument()
    expect(screen.getByText('TypeScript')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /ver publicaciones de la comunidad/i })).toHaveAttribute('href', '/posts')
    expect(mockedGetProject).toHaveBeenCalledWith('project-1', expect.any(AbortSignal))
  })

  it('allows retrying after project loading fails', async () => {
    mockedGetProject.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(project)
    const user = userEvent.setup()
    renderProject()
    expect(await screen.findByRole('alert')).toHaveTextContent(/no pudimos cargar el proyecto/i)
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('heading', { name: 'Compiler' })).toBeInTheDocument()
  })
})
