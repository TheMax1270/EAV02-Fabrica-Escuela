import { useEffect, useState, type FormEvent } from 'react'
import { createProject, getMyProjects, updateProject, type Project, type ProjectPayload, type ProjectStatus } from '../api/project'
import SiteHeader from '../components/SiteHeader'
import TextField from '../components/TextField'
import { Link } from 'react-router-dom'

const statusLabels: Record<ProjectStatus, string> = {
  IN_PROGRESS: 'En desarrollo',
  COMPLETED: 'Finalizado',
  LOOKING_FOR_COLLABORATORS: 'Buscando colaboradores',
}

const statusClasses: Record<ProjectStatus, string> = {
  IN_PROGRESS: 'project-status-in-progress',
  COMPLETED: 'project-status-completed',
  LOOKING_FOR_COLLABORATORS: 'project-status-collaborators',
}

const emptyValues: ProjectPayload = {
  title: '', description: '', technologies: [], status: 'IN_PROGRESS', repositoryUrl: null,
}

function listText(values: string[]) {
  return values.join(', ')
}

function parseList(value: string) {
  return value.split(',').map((item) => item.trim()).filter(Boolean)
}

export default function MyProjectsPage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<string | 'new' | null>(null)
  const [values, setValues] = useState<ProjectPayload>(emptyValues)
  const [technologies, setTechnologies] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let cancelled = false
    getMyProjects().then((result) => {
      if (!cancelled) { setProjects(result); setError(null) }
    }).catch(() => {
      if (!cancelled) setError('No pudimos cargar tus proyectos.')
    }).finally(() => {
      if (!cancelled) setIsLoading(false)
    })
    return () => { cancelled = true }
  }, [reload])

  function startCreate() {
    setValues(emptyValues)
    setTechnologies('')
    setFormError(null)
    setEditingId('new')
  }

  function startEdit(project: Project) {
    setValues({ title: project.title, description: project.description, technologies: project.technologies,
      status: project.status, repositoryUrl: project.repositoryUrl })
    setTechnologies(listText(project.technologies))
    setFormError(null)
    setEditingId(project.id)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSaving || !editingId) return
    const payload: ProjectPayload = { ...values, technologies: parseList(technologies) }
    if (!payload.title.trim() || !payload.description.trim() || payload.technologies.length === 0) {
      setFormError('Completa el título, la descripción y al menos una tecnología.')
      return
    }
    setIsSaving(true)
    setFormError(null)
    try {
      if (editingId === 'new') await createProject(payload)
      else await updateProject(editingId, payload)
      setEditingId(null)
      setIsLoading(true)
      setReload((current) => current + 1)
    } catch {
      setFormError('No pudimos guardar el proyecto. Revisa los datos e inténtalo de nuevo.')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="account-main content-width">
        <div className="page-heading">
          <p className="eyebrow">TU TRABAJO</p>
          <h1>Mis proyectos</h1>
          <p>Publica y actualiza los proyectos técnicos que quieres mostrar a la comunidad.</p>
        </div>

        {!editingId && (
          <button className="button button-primary" type="button" onClick={startCreate}>
            Publicar nuevo proyecto
          </button>
        )}

        {isLoading && <p className="loading-note" role="status">Cargando proyectos...</p>}
        {error && <p className="form-error" role="alert">{error}</p>}

        {editingId && (
          <section className="auth-panel profile-form-panel" aria-label="Formulario de proyecto">
            <h2>{editingId === 'new' ? 'Publicar proyecto' : 'Editar proyecto'}</h2>
            {formError && <p className="form-error" role="alert">{formError}</p>}
            <form onSubmit={handleSubmit} noValidate aria-busy={isSaving}>
              <TextField name="title" label="Título" autoComplete="off"
                value={values.title} onChange={(value) => setValues((current) => ({ ...current, title: value }))}
                maxLength={150} disabled={isSaving} />
              <div className="form-field">
                <label htmlFor="description">Descripción</label>
                <textarea id="description" value={values.description} maxLength={2000} rows={4}
                  onChange={(event) => setValues((current) => ({ ...current, description: event.target.value }))}
                  disabled={isSaving} required />
              </div>
              <TextField name="technologies" label="Tecnologías (separadas por coma)" autoComplete="off"
                value={technologies} onChange={setTechnologies} disabled={isSaving} />
              <div className="form-field">
                <label htmlFor="status">Estado</label>
                <select id="status" value={values.status}
                  onChange={(event) => setValues((current) => ({ ...current, status: event.target.value as ProjectStatus }))}
                  disabled={isSaving}>
                  {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
              </div>
              <TextField name="repositoryUrl" label="Repositorio GitHub/GitLab (opcional)" autoComplete="url"
                value={values.repositoryUrl ?? ''}
                onChange={(value) => setValues((current) => ({ ...current, repositoryUrl: value || null }))}
                disabled={isSaving} />
              <div className="hero-actions">
                <button className="button button-primary" type="submit" disabled={isSaving}>
                  {isSaving ? 'Guardando...' : editingId === 'new' ? 'Publicar' : 'Guardar cambios'}
                </button>
                <button className="button button-secondary" type="button" onClick={() => setEditingId(null)} disabled={isSaving}>
                  Cancelar
                </button>
              </div>
            </form>
          </section>
        )}

        {!isLoading && !error && !editingId && (
          <div className="projects-grid">
            {projects.map((project) => (
              <article className="project-card" key={project.id}>
                <div className="project-card-header">
                  <h2>{project.title}</h2>
                  <span className={`project-status ${statusClasses[project.status]}`}>
                    {statusLabels[project.status]}
                  </span>
                </div>
                <div className="project-field">
                  <h3>Descripción</h3>
                  <p>{project.description}</p>
                </div>
                <div className="project-field">
                  <h3>Tecnologías utilizadas</h3>
                  <div className="skill-list">
                    {project.technologies.map((tech) => <span key={tech}>{tech}</span>)}
                  </div>
                </div>
                <div className="project-meta">
                  <div className="project-field">
                    <h3>Estado</h3>
                    <p>{statusLabels[project.status]}</p>
                  </div>
                  <div className="project-field">
                    <h3>Repositorio</h3>
                    {project.repositoryUrl ? (
                      <a className="project-repository-link" href={project.repositoryUrl} target="_blank" rel="noreferrer">
                        GitHub/GitLab ↗
                      </a>
                    ) : (
                      <p>No registrado</p>
                    )}
                  </div>
                </div>
                <div className="project-card-actions">
                  <button className="button button-secondary" type="button" onClick={() => startEdit(project)}>
                    Editar proyecto
                  </button>
                </div>
              </article>
            ))}
            {projects.length === 0 && <p className="projects-empty">Aún no has publicado proyectos.</p>}
          </div>
        )}

        <p className="auth-switch"><Link to="/account">Volver a mi cuenta</Link></p>
      </main>
    </div>
  )
}
