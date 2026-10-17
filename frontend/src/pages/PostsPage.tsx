import { useEffect, useRef, useState, type FormEvent } from 'react'
import axios from 'axios'
import { Link } from 'react-router-dom'
import { createPost, listPosts, resolvePost, deletePost, type Post, type PostPage, type PostPayload } from '../api/post'
import type { ApiErrorResponse } from '../api/auth'
import SiteHeader from '../components/SiteHeader'
import TextField from '../components/TextField'
import InteractionPanel from '../components/InteractionPanel'
import { useAuth } from '../context/useAuth'
import ReportButton from '../components/ReportButton'
import { postReportStatus, reportPost } from '../api/report'

const PAGE_SIZE = 20
type FieldErrors = { title?: string; content?: string; link?: string; categories?: string }

function validLink(value: string) {
  try {
    const url = new URL(value)
    return /^https?:\/\/\S+$/.test(value) && Boolean(url.hostname) && !url.username && !url.password
  } catch { return false }
}

function errorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    if (error.response?.status === 401) return 'Tu sesión no está disponible. Inicia sesión para continuar.'
    if (error.response?.status === 403) return 'No tienes permiso para realizar esta acción.'
    if (error.response?.status === 409) return error.response.data?.message || 'Esta publicación no puede resolverse.'
    if (error.response?.status === 400) return error.response.data?.message || 'Revisa el título y el contenido.'
  }
  return fallback
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('es-CO', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

export default function PostsPage() {
  const { user, isAuthenticated } = useAuth()
  const [data, setData] = useState<PostPage | null>(null)
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [title, setTitle] = useState('')
  const [type, setType] = useState<Post['type']>('GENERAL')
  const [link, setLink] = useState('')
  const [categories, setCategories] = useState('')
  const [content, setContent] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [publishError, setPublishError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [isPublishing, setIsPublishing] = useState(false)
  const [composerOpen, setComposerOpen] = useState(false)
  const publishing = useRef(false)
  const resolving = useRef(false)
  const [resolvingId, setResolvingId] = useState<string | null>(null)
  const [resolveError, setResolveError] = useState<{ id: string; message: string } | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<{ id: string; message: string } | null>(null)
  const [deleted, setDeleted] = useState(false)
  const deleting = useRef(false)

  useEffect(() => {
    const controller = new AbortController()
    listPosts(page, PAGE_SIZE, controller.signal).then((result) => {
      if (!controller.signal.aborted) { setData(result); setLoadError(null) }
    }).catch((error: unknown) => {
      if (!controller.signal.aborted) {
        setLoadError(errorMessage(error, 'No pudimos cargar las publicaciones. Intenta de nuevo.'))
      }
    }).finally(() => {
      if (!controller.signal.aborted) setIsLoading(false)
    })
    return () => controller.abort()
  }, [page, reload])

  function refreshList(nextPage: number) {
    setIsLoading(true)
    setLoadError(null)
    setPage(nextPage)
    setReload((current) => current + 1)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (publishing.current || !isAuthenticated) return
    const technical = type === 'TECHNICAL_DISCUSSION'
    const tags = categories.trim() ? categories.split(',').map((value) => value.trim()) : []
    const payload: PostPayload = { type, content: content.trim(),
      ...(technical ? { title: title.trim(), categories: tags } : { ...(link.trim() ? { link: link.trim() } : {}) }) }
    const errors: FieldErrors = {}
    if (technical && !payload.title) errors.title = 'Escribe un título para tu discusión.'
    else if (payload.title && payload.title.length > 150) errors.title = 'El título debe tener como máximo 150 caracteres.'
    if (!payload.content) errors.content = 'Escribe el contenido de tu publicación.'
    else if (payload.content.length > 5000) errors.content = 'El contenido debe tener como máximo 5000 caracteres.'
    if (technical && (tags.length === 0 || tags.some((value) => !value))) errors.categories = 'Indica al menos una categoría o tecnología; evita valores vacíos.'
    else if (technical && (tags.length > 20 || tags.some((value) => value.length > 50))) errors.categories = 'Usa hasta 20 categorías, de máximo 50 caracteres cada una.'
    if (!technical && link.trim() && (!validLink(link.trim()) || link.trim().length > 500)) errors.link = 'Usa una URL http:// o https:// válida de máximo 500 caracteres.'
    setFieldErrors(errors)
    setPublishError(null)
    setSuccess(false)
    if (Object.keys(errors).length) return
    publishing.current = true
    setIsPublishing(true)
    try {
      await createPost(payload)
      setTitle('')
      setContent('')
      setLink('')
      setCategories('')
      setFieldErrors({})
      setSuccess(true)
      setComposerOpen(false)
      refreshList(0)
    } catch (error: unknown) {
      if (axios.isAxiosError<ApiErrorResponse>(error) && error.response?.status === 400) {
        const errors = error.response.data?.errors
        setFieldErrors({ title: errors?.title, content: errors?.content, link: errors?.link,
          categories: errors?.categories || Object.entries(errors || {}).find(([key]) => key.startsWith('categories['))?.[1] })
      }
      setPublishError(errorMessage(error, 'No pudimos publicar. Tu texto se conserva para que puedas intentarlo de nuevo.'))
    } finally {
      publishing.current = false
      setIsPublishing(false)
    }
  }

  async function handleResolve(id: string) {
    if (resolving.current) return
    resolving.current = true
    setResolvingId(id)
    setResolveError(null)
    try {
      const updated = await resolvePost(id)
      setData((current) => current ? { ...current, content: current.content.map((post) => post.id === id ? updated : post) } : current)
    } catch (error: unknown) {
      setResolveError({ id, message: errorMessage(error, 'No pudimos resolver la discusión. Intenta de nuevo.') })
    } finally {
      resolving.current = false
      setResolvingId(null)
    }
  }

  async function handleDelete(id: string) {
    if (deleting.current || !isAuthenticated) return
    if (!window.confirm('¿Eliminar esta publicación? También se eliminarán sus comentarios, respuestas y Me gusta. Esta acción no se puede deshacer.')) return
    deleting.current = true
    setDeletingId(id)
    setDeleteError(null)
    setDeleted(false)
    try {
      await deletePost(id)
      setDeleted(true)
      const nextPage = data?.content.length === 1 && page > 0 ? page - 1 : page
      setData((current) => current ? { ...current, content: current.content.filter((post) => post.id !== id) } : current)
      refreshList(nextPage)
    } catch (error: unknown) {
      setDeleteError({ id, message: errorMessage(error, 'No pudimos eliminar la publicación. Intenta de nuevo.') })
    } finally {
      deleting.current = false
      setDeletingId(null)
    }
  }

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="account-main content-width posts-main">
        <div className="page-heading posts-heading">
          <p className="eyebrow">IDEAS · CÓDIGO · COMUNIDAD</p>
          <h1>Publicaciones y discusiones</h1>
          <p>Comparte lo que aprendes, plantea una idea y abre una conversación técnica.</p>
        </div>

        {isAuthenticated ? (
          !composerOpen ? <button className="post-composer-trigger" type="button" onClick={() => setComposerOpen(true)}>
            <span className="post-avatar" aria-hidden="true">{user?.username.charAt(0).toUpperCase()}</span>
            <span>Crear publicación</span>
          </button> : <section className="post-composer" aria-labelledby="composer-title">
            <div className="post-composer-heading">
              <span className="post-avatar" aria-hidden="true">{user?.username.charAt(0).toUpperCase()}</span>
              <div><h2 id="composer-title">Comparte con la comunidad</h2><p>Publicando como @{user?.username}</p></div>
            </div>
            <form onSubmit={handleSubmit} noValidate aria-busy={isPublishing}>
              <div className="form-field">
                <label htmlFor="post-type">¿Qué quieres compartir?</label>
                <select id="post-type" value={type} disabled={isPublishing}
                  onChange={(event) => { setType(event.target.value as Post['type']); setFieldErrors({}); setPublishError(null); setSuccess(false) }}>
                  <option value="GENERAL">Actualización general</option>
                  <option value="TECHNICAL_DISCUSSION">Discusión técnica</option>
                </select>
              </div>
              {type === 'TECHNICAL_DISCUSSION' && <TextField name="post-title" label="Título" autoComplete="off" value={title}
                onChange={(value) => { setTitle(value); setFieldErrors((current) => ({ ...current, title: undefined })); setSuccess(false) }}
                maxLength={150} error={fieldErrors.title} disabled={isPublishing} />}
              <div className="form-field">
                <label htmlFor="post-content">{type === 'GENERAL' ? 'Contenido' : 'Descripción'}</label>
                <textarea id="post-content" value={content} rows={6} maxLength={5000} required
                  disabled={isPublishing} aria-invalid={Boolean(fieldErrors.content)}
                  aria-describedby={fieldErrors.content ? 'post-content-error post-content-count' : 'post-content-count'}
                  onChange={(event) => { setContent(event.target.value); setFieldErrors((current) => ({ ...current, content: undefined })); setSuccess(false) }} />
                {fieldErrors.content && <p className="field-error" id="post-content-error">{fieldErrors.content}</p>}
                <p className="post-character-count" id="post-content-count">{content.length.toLocaleString('es-CO')} / 5.000 caracteres</p>
              </div>
              {type === 'GENERAL' ? <div className="form-field">
                <label htmlFor="post-link">Enlace (opcional)</label>
                <input id="post-link" type="url" value={link} maxLength={500} disabled={isPublishing}
                  autoComplete="url" aria-invalid={Boolean(fieldErrors.link)} aria-describedby={fieldErrors.link ? 'post-link-error' : undefined}
                  onChange={(event) => { setLink(event.target.value); setFieldErrors((current) => ({ ...current, link: undefined })); setSuccess(false) }} />
                {fieldErrors.link && <p className="field-error" id="post-link-error">{fieldErrors.link}</p>}
              </div> : <div className="form-field">
                <TextField name="post-categories" label="Categorías o tecnologías (separadas por coma)" autoComplete="off" value={categories}
                  onChange={(value) => { setCategories(value); setFieldErrors((current) => ({ ...current, categories: undefined })); setSuccess(false) }}
                  error={fieldErrors.categories} disabled={isPublishing} />
                <p className="post-character-count">Al menos una. Hasta 20 categorías, de 50 caracteres cada una.</p>
              </div>}
              {publishError && <p className="form-error" role="alert">{publishError}</p>}
              <div className="post-composer-actions">
                <span className="post-character-count">{type === 'TECHNICAL_DISCUSSION' ? `Título: ${title.length} / 150` : 'Comparte una actualización con la comunidad'}</span>
                <div className="post-composer-buttons">
                  <button className="button button-secondary" type="button" disabled={isPublishing} onClick={() => setComposerOpen(false)}>Cancelar</button>
                  <button className="button button-primary" type="submit" disabled={isPublishing}>{isPublishing ? 'Publicando...' : 'Publicar'}</button>
                </div>
              </div>
            </form>
          </section>
        ) : (
          <div className="post-login-note"><p>Las ideas se comparten mejor en comunidad. <Link to="/login">Inicia sesión</Link> para publicar.</p></div>
        )}
        {success && <p className="post-success" role="status">Publicación creada correctamente.</p>}
        {deleted && <p className="post-success" role="status">Publicación eliminada.</p>}

        <section className="post-feed" aria-labelledby="feed-title" aria-busy={isLoading}>
          <div className="post-feed-heading">
            <h2 id="feed-title">En la comunidad</h2>
            <span>Más recientes primero</span>
          </div>
          {isLoading && <p className="loading-note" role="status">Cargando publicaciones...</p>}
          {loadError && <div className="form-error" role="alert"><p>{loadError}</p>
            <button className="text-button" type="button" disabled={isLoading} onClick={() => refreshList(page)}>Reintentar</button>
          </div>}
          {!isLoading && !loadError && data && (
            <>
              {data.content.length === 0 ? (
                <div className="post-empty"><span aria-hidden="true">&lt;/&gt;</span>
                  <h3>Aún no hay publicaciones</h3><p>Comparte una idea o algo que hayas aprendido para abrir la conversación.</p>
                </div>
              ) : data.content.map((post) => (
                <article className="post-card" key={post.id}>
                  <header className="post-author">
                    <span className="post-avatar" aria-hidden="true">{post.username.charAt(0).toUpperCase()}</span>
                    <div><Link className="post-username" to={`/developers/${post.userId}`}>@{post.username}</Link>
                      <time dateTime={post.createdAt}>{formatDate(post.createdAt)}</time></div>
                  </header>
                  <div className="post-badges"><span className={`post-type post-type-${post.type === 'GENERAL' ? 'general' : 'technical'}`}>
                    {post.type === 'GENERAL' ? 'Actualización general' : 'Discusión técnica'}</span>
                    {post.resolved && <span className="post-resolved">Resuelta</span>}
                  </div>
                  {post.title && <h3>{post.title}</h3>}
                  <p className="post-content">{post.content}</p>
                  {post.type === 'GENERAL' && post.link && validLink(post.link) && <a className="post-link" href={post.link} target="_blank" rel="noreferrer">{post.link}</a>}
                  {post.type === 'TECHNICAL_DISCUSSION' && <div className="skill-list post-categories">
                    {post.categories.map((category, index) => <span key={`${category}-${index}`}>{category}</span>)}
                  </div>}
                  {isAuthenticated && user?.id === post.userId && post.type === 'TECHNICAL_DISCUSSION' && !post.resolved &&
                    <button className="button button-secondary post-resolve" type="button" disabled={resolvingId !== null}
                      onClick={() => void handleResolve(post.id)}>{resolvingId === post.id ? 'Resolviendo...' : 'Marcar como resuelta'}</button>}
                  {resolveError?.id === post.id && <p className="form-error" role="alert">{resolveError.message}</p>}
                  {isAuthenticated && user?.id === post.userId &&
                    <button className="button button-danger post-resolve" type="button" disabled={deletingId !== null}
                      onClick={() => void handleDelete(post.id)}>{deletingId === post.id ? 'Eliminando...' : 'Eliminar'}</button>}
                  {deleteError?.id === post.id && <p className="form-error" role="alert">{deleteError.message}</p>}
                  {user?.id !== post.userId && <ReportButton statusCheck={() => postReportStatus(post.id)}
                    onSubmit={(payload) => reportPost(post.id, payload)} />}
                  <InteractionPanel target={{ kind: 'posts', id: post.id }} />
                </article>
              ))}
              {data.totalPages > 1 && <nav className="pagination" aria-label="Paginación de publicaciones">
                <button className="button button-secondary" type="button" disabled={page === 0 || isPublishing}
                  onClick={() => refreshList(page - 1)}>Anterior</button>
                <span>Página {data.page + 1} de {data.totalPages}</span>
                <button className="button button-secondary" type="button" disabled={page >= data.totalPages - 1 || isPublishing}
                  onClick={() => refreshList(page + 1)}>Siguiente</button>
              </nav>}
            </>
          )}
        </section>
      </main>
    </div>
  )
}
