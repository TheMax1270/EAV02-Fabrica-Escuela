import { useEffect, useState } from 'react'
import axios from 'axios'
import {
  dismissReport, listPendingReports, listResolvedReports, removeReportedContent, restoreReport, reviewReport,
  type AdminReport, type ModerationStatus, type ReportReason, type ReportTargetType,
} from '../api/moderation'
import type { ApiErrorResponse } from '../api/auth'
import SiteHeader from '../components/SiteHeader'

const PAGE_SIZE = 20

const targetLabels: Record<ReportTargetType, string> = {
  POST: 'Publicación', COMMENT: 'Comentario', PROFILE: 'Perfil',
}

const reasonLabels: Record<ReportReason, string> = {
  SPAM: 'Spam', HARASSMENT: 'Acoso', INAPPROPRIATE_CONTENT: 'Contenido inapropiado',
  MISINFORMATION: 'Desinformación', OTHER: 'Otro',
}

const statusLabels: Record<ModerationStatus, string> = {
  VISIBLE: 'Visible', IN_REVIEW: 'En revisión', REMOVED: 'Retirado',
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('es-CO', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

function errorMessage(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error) && error.response?.data?.message) {
    return error.response.data.message
  }
  return 'No pudimos completar la acción. Actualiza la lista e inténtalo de nuevo.'
}

export default function AdminModerationPage() {
  const [page, setPage] = useState(0)
  const [view, setView] = useState<'pending' | 'resolved'>('pending')
  const [data, setData] = useState<{ content: AdminReport[]; totalElements: number; totalPages: number } | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [pendingId, setPendingId] = useState<string | null>(null)
  const [confirmation, setConfirmation] = useState<{ report: AdminReport; action: 'review' | 'restore' | 'remove' | 'dismiss' } | null>(null)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let cancelled = false
    setIsLoading(true)
    const request = view === 'pending' ? listPendingReports(page, PAGE_SIZE) : listResolvedReports(page, PAGE_SIZE)
    request.then((result) => {
      if (cancelled) return
      setData(result)
      setError(null)
    }).catch((requestError: unknown) => {
      if (!cancelled) setError(errorMessage(requestError))
    }).finally(() => {
      if (!cancelled) setIsLoading(false)
    })
    return () => { cancelled = true }
  }, [page, reload, view])

  function handleAction(report: AdminReport, action: 'review' | 'restore' | 'remove' | 'dismiss') {
    setConfirmation({ report, action })
  }

  async function executeAction(report: AdminReport, action: 'review' | 'restore' | 'remove' | 'dismiss') {
    if (pendingId) return
    setConfirmation(null)
    setPendingId(report.id)
    setActionError(null)
    try {
      const updated = action === 'review' ? await reviewReport(report.id)
        : action === 'restore' ? await restoreReport(report.id)
          : action === 'remove' ? await removeReportedContent(report.id)
            : await dismissReport(report.id)
      if (action === 'review') {
        setData((current) => current ? { ...current, content: current.content.map((item) => item.id === updated.id ? updated : item) } : current)
      } else {
        const nextPage = data?.content.length === 1 && page > 0 ? page - 1 : page
        setPage(nextPage)
        setReload((current) => current + 1)
      }
    } catch (requestError) {
      setActionError(errorMessage(requestError))
    } finally {
      setPendingId(null)
    }
  }

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="account-main content-width">
        <div className="page-heading">
          <p className="eyebrow">ADMINISTRACIÓN</p>
          <h1>Moderación de reportes</h1>
          <p>Revisa reportes pendientes, aplica una decisión y conserva la trazabilidad.</p>
        </div>

        <div className="moderation-tabs" role="tablist" aria-label="Vistas de reportes">
          <button className={`moderation-tab ${view === 'pending' ? 'active' : ''}`} type="button"
            role="tab" aria-selected={view === 'pending'} onClick={() => { setView('pending'); setPage(0); setReload((current) => current + 1) }}>
            Pendientes
          </button>
          <button className={`moderation-tab ${view === 'resolved' ? 'active' : ''}`} type="button"
            role="tab" aria-selected={view === 'resolved'} onClick={() => { setView('resolved'); setPage(0); setReload((current) => current + 1) }}>
            Resueltos
          </button>
        </div>
        {actionError && <p className="form-error" role="alert">{actionError}</p>}
        {isLoading && <p className="loading-note" role="status">Cargando reportes...</p>}
        {error && <div className="form-error" role="alert"><span>{error}</span>
          <button className="text-button" type="button" onClick={() => setReload((current) => current + 1)}>Reintentar</button>
        </div>}
        {data && !error && <>
          <p className="admin-count" role="status">{data.totalElements} reportes {view === 'pending' ? 'pendientes' : 'resueltos'}</p>
          <div className="moderation-list">
            {data.content.length === 0 && <p className="admin-empty">No hay reportes pendientes.</p>}
            {data.content.map((report) => (
              <article className="moderation-card" key={report.id}>
                <div className="moderation-card-heading">
                  <div>
                    <span className="post-type post-type-technical">{targetLabels[report.targetType]}</span>
                    <h2>{report.targetUsername ? `@${report.targetUsername}` : 'Elemento reportado'}</h2>
                  </div>
                  <span className={`status-pill ${report.moderationStatus === 'IN_REVIEW' ? 'status-review' : 'status-active'}`}>
                    {report.moderationStatus ? statusLabels[report.moderationStatus] : 'No disponible'}
                  </span>
                </div>
                <p className="moderation-preview">{report.targetPreview}</p>
                <dl className="moderation-meta">
                  <div><dt>Motivo</dt><dd>{reasonLabels[report.reason]}</dd></div>
                  <div><dt>Reportó</dt><dd>@{report.reporterUsername}</dd></div>
                  <div><dt>Fecha</dt><dd>{formatDate(report.createdAt)}</dd></div>
                </dl>
                {report.details && <p className="moderation-details"><strong>Detalle:</strong> {report.details}</p>}
                {view === 'pending' && <div className="moderation-actions">
                  {report.moderationStatus === 'VISIBLE' && <button className="button button-secondary" type="button"
                    disabled={pendingId !== null} onClick={() => void handleAction(report, 'review')}>
                    {pendingId === report.id ? 'Guardando...' : 'En revisión'}
                  </button>}
                  {report.moderationStatus === 'IN_REVIEW' && <button className="button button-secondary" type="button"
                    disabled={pendingId !== null} onClick={() => void handleAction(report, 'restore')}>
                    {pendingId === report.id ? 'Guardando...' : 'Restaurar'}
                  </button>}
                  {report.moderationStatus && report.moderationStatus !== 'REMOVED' && <button className="button button-danger" type="button"
                    disabled={pendingId !== null} onClick={() => void handleAction(report, 'remove')}>
                    Aceptar reporte
                  </button>}
                  <button className="button button-secondary" type="button" disabled={pendingId !== null}
                    onClick={() => void handleAction(report, 'dismiss')}>Descartar reporte</button>
                </div>}
              </article>
            ))}
          </div>
          {data.totalPages > 1 && <nav className="pagination" aria-label="Paginación de reportes">
            <button className="button button-secondary" type="button" disabled={isLoading || page === 0}
              onClick={() => setPage((current) => current - 1)}>Anterior</button>
            <span>Página {page + 1} de {data.totalPages}</span>
            <button className="button button-secondary" type="button" disabled={isLoading || page >= data.totalPages - 1}
              onClick={() => setPage((current) => current + 1)}>Siguiente</button>
          </nav>}
        </>}
      </main>
      {confirmation && (
        <div className="modal-overlay" role="presentation">
          <div className="confirm-modal" role="dialog" aria-modal="true" aria-labelledby="moderation-confirm-title">
            <p className="eyebrow">ACCIÓN DE MODERACIÓN</p>
            <h2 id="moderation-confirm-title">
              {confirmation.action === 'review' ? 'Enviar a revisión' : confirmation.action === 'restore' ? 'Restaurar contenido' : confirmation.action === 'remove' ? 'Retirar contenido' : 'Descartar reporte'}
            </h2>
            <p>
              {confirmation.action === 'review'
                ? 'El contenido dejará de estar visible para la comunidad mientras se revisa.'
                : confirmation.action === 'restore'
                  ? 'El contenido volverá a estar disponible públicamente y el reporte se resolverá.'
                  : confirmation.action === 'remove'
                    ? 'El contenido se ocultará mediante eliminación lógica y el reporte se resolverá.'
                    : 'El reporte se marcará como descartado y el contenido permanecerá sin cambios.'}
            </p>
            <div className="confirm-modal-actions">
              <button className="button button-secondary" type="button" disabled={Boolean(pendingId)} onClick={() => setConfirmation(null)}>Cancelar</button>
              <button className={`button ${confirmation.action === 'remove' ? 'button-danger' : 'button-primary'}`} type="button"
                disabled={Boolean(pendingId)} onClick={() => void executeAction(confirmation.report, confirmation.action)}>
                {pendingId ? 'Guardando...' : 'Confirmar acción'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
