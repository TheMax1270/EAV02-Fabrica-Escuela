import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getProject, type Project } from '../api/project'
import SiteHeader from '../components/SiteHeader'
import InteractionPanel from '../components/InteractionPanel'
import { interactionError } from '../api/interaction'
export default function ProjectDetailPage() {
  const { projectId } = useParams()
  const [project, setProject] = useState<Project | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [reload, setReload] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    if (projectId) getProject(projectId, controller.signal).then((result) => {
      if (!controller.signal.aborted) { setProject(result); setError(null) }
    }).catch((failure: unknown) => {
      if (!controller.signal.aborted) setError(interactionError(failure, 'No pudimos cargar el proyecto.'))
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [projectId, reload])
  return <div className="app-shell"><SiteHeader /><main className="account-main content-width posts-main">
    <p className="eyebrow">PROYECTO DE LA COMUNIDAD</p>
    {loading && <p role="status">Cargando proyecto...</p>}
    {error && <div className="form-error" role="alert"><p>{error}</p><button className="text-button" type="button"
      onClick={() => { setLoading(true); setReload((value) => value + 1) }}>Reintentar</button></div>}
    {!loading && !error && project && <article className="post-card">
      <p className="post-username">@{project.username}</p><h1>{project.title}</h1>
      <p className="post-content">{project.description}</p>
      <div className="skill-list post-categories">{project.technologies.map((tech, index) => <span key={index}>{tech}</span>)}</div>
      <InteractionPanel key={project.id} target={{ kind: 'projects', id: project.id }} />
    </article>}
    <p className="auth-switch"><Link to="/posts">Ver publicaciones de la comunidad</Link></p>
  </main></div>
}
