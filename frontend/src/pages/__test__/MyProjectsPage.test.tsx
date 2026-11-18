import '@testing-library/jest-dom/vitest'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import MyProjectsPage from '../MyProjectsPage'
import { createProject, getMyProjects } from '../../api/project'

vi.mock('../../api/project', () => ({
  createProject: vi.fn(), getMyProjects: vi.fn(), updateProject: vi.fn(),
}))
vi.mock('../../components/SiteHeader', () => ({ default: () => <header /> }))
vi.mock('../../components/InteractionPanel', () => ({ default: () => <div /> }))

const mockedProjects = vi.mocked(getMyProjects)
const mockedCreateProject = vi.mocked(createProject)
const project = {
  id: 'project-1', userId: 'user-1', username: 'ada', title: 'Compiler',
  description: 'A small compiler', technologies: ['TypeScript'], status: 'IN_PROGRESS' as const,
  repositoryUrl: 'https://github.com/ada/compiler', createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('MyProjectsPage', () => {
  beforeEach(() => {
    mockedProjects.mockReset().mockResolvedValue([project])
    mockedCreateProject.mockReset().mockResolvedValue(project)
  })

  it('shows projects and validates and submits the create-project form', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><MyProjectsPage /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: 'Compiler' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /ver proyecto público/i })).toHaveAttribute('href', '/projects/project-1')
    await user.click(screen.getByRole('button', { name: /publicar nuevo proyecto/i }))
    await user.click(screen.getByRole('button', { name: 'Publicar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/completa el título/i)
    expect(mockedCreateProject).not.toHaveBeenCalled()

    await user.type(screen.getByLabelText('Título'), 'Nueva herramienta')
    await user.type(screen.getByLabelText('Descripción'), 'Una herramienta para la comunidad')
    await user.type(screen.getByLabelText(/tecnologías/i), 'React, TypeScript')
    await user.click(screen.getByRole('button', { name: 'Publicar' }))
    expect(await screen.findByRole('heading', { name: 'Compiler' })).toBeInTheDocument()
    expect(mockedCreateProject).toHaveBeenCalledWith({
      title: 'Nueva herramienta', description: 'Una herramienta para la comunidad',
      technologies: ['React', 'TypeScript'], status: 'IN_PROGRESS', repositoryUrl: null,
    })
  })

  it('displays a load error instead of an empty project list', async () => {
    mockedProjects.mockRejectedValueOnce(new Error('offline'))
    render(<MemoryRouter><MyProjectsPage /></MemoryRouter>)
    expect(await screen.findByRole('alert')).toHaveTextContent(/no pudimos cargar tus proyectos/i)
  })
})
